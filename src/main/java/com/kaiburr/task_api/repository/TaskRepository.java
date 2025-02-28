package com.kaiburr.task_api.repository;

import com.kaiburr.task_api.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    /**
     * Finds tasks where the name contains the specified string (case-sensitive).
     * @param name The substring to search for in task names
     * @return List of matching tasks
     */
    List<Task> findByNameContaining(String name);
}