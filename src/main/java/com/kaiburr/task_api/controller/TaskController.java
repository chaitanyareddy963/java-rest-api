package com.kaiburr.task_api.controller;

import com.kaiburr.task_api.model.Task;
import com.kaiburr.task_api.model.TaskExecution;
import com.kaiburr.task_api.repository.TaskRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;

/**
 * REST controller for managing tasks and their executions.
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {
	private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskRepository taskRepository;

    @PostConstruct
    public void init() {
        logger.info("TaskController initialized");
    }
    
    private static final List<String> FORBIDDEN_COMMANDS = Arrays.asList("rm", "mkfs", "dd", "reboot", "shutdown");

    /**
     * GET /tasks: Retrieves all tasks or searches by name if 'search' parameter is provided.
     * @param search Optional query parameter to filter tasks by name
     * @return List of tasks or 404 if search yields no results
     */
    @GetMapping
    public ResponseEntity<List<Task>> getTasks(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            List<Task> tasks = taskRepository.findByNameContaining(search);
            return tasks.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(tasks);
        }
        List<Task> allTasks = taskRepository.findAll();
        return ResponseEntity.ok(allTasks);
    }

    /**
     * GET /tasks/{id}: Retrieves a task by its ID.
     * @param id The task ID
     * @return Task object or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        Optional<Task> task = taskRepository.findById(id);
        return task.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * PUT /tasks/{id}: Creates or updates a task with the given ID.
     * Validates the command for disallowed keywords.
     * @param id The task ID in the URL
     * @param task The task object in the request body
     * @return Saved task or 400 if validation fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> createOrUpdateTask(@PathVariable String id, @RequestBody Task task) {
        if (!id.equals(task.getId()) || task.getCommand() == null || isMaliciousCommand(task.getCommand())) {
            return ResponseEntity.badRequest().build();
        }
        Task savedTask = taskRepository.save(task);
        return ResponseEntity.ok(savedTask);
    }

    /**
     * DELETE /tasks/{id}: Deletes a task by its ID.
     * @param id The task ID
     * @return 204 on success, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * PUT /tasks/{id}/executions: Executes the task's command in a Kubernetes pod and stores the execution details.
     * @param id The task ID
     * @return TaskExecution object or 404 if task not found
     * @throws Exception If Kubernetes interaction fails
     */
    @PutMapping("/{id}/executions")
    public ResponseEntity<TaskExecution> executeTask(@PathVariable String id) throws Exception {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (!optionalTask.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Task task = optionalTask.get();

        // Initialize Kubernetes client
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        String podName = "task-execution-" + UUID.randomUUID().toString();
        V1Pod pod = new V1Pod()
                .metadata(new V1ObjectMeta().name(podName))
                .spec(new V1PodSpec()
                        .containers(Arrays.asList(
                                new V1Container()
                                        .name("executor")
                                        .image("busybox")
                                        .command(Arrays.asList("/bin/sh", "-c", task.getCommand()))
                        ))
                        .restartPolicy("Never"));

        // Create pod
        api.createNamespacedPod("default", pod, null, null, null);

        // Wait for pod completion
        V1Pod currentPod;
        do {
            Thread.sleep(1000);
            currentPod = api.readNamespacedPod(podName, "default", null, null, null);
        } while (!"Succeeded".equals(currentPod.getStatus().getPhase()) && !"Failed".equals(currentPod.getStatus().getPhase()));

        // Capture output and times
        String output = api.readNamespacedPodLog(podName, "default", null, null, null, null, null, null, null, null, null);
        Date startTime = Date.from(currentPod.getStatus().getStartTime().toInstant());
        V1ContainerStateTerminated terminated = currentPod.getStatus().getContainerStatuses().get(0).getState().getTerminated();
        Date endTime = terminated != null ? Date.from(terminated.getFinishedAt().toInstant()) : new Date();

        // Create and store TaskExecution
        TaskExecution execution = new TaskExecution(startTime, endTime, output);
        task.getTaskExecutions().add(execution);
        taskRepository.save(task);

        // Clean up pod
        api.deleteNamespacedPod(podName, "default", null, null, null, null, null, null);

        return ResponseEntity.ok(execution);
    }

    /**
     * Checks if the command contains forbidden keywords.
     * @param command The shell command to validate
     * @return True if malicious, false otherwise
     */
    private boolean isMaliciousCommand(String command) {
        return FORBIDDEN_COMMANDS.stream().anyMatch(command.toLowerCase()::contains);
    }
}