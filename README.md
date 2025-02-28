# Java REST API  End-to-End Process Guide and Documentation

## Introduction

The Java REST API is a Spring Boot application designed to manage and execute tasks. Tasks are defined as shell commands that are executed within Kubernetes pods. This application allows users to create, retrieve, update, delete, and execute tasks, with task data persistently stored in a MongoDB database.

## Application Workflow and Architecture

### High-Level Overview

1. **User Interaction:** Users interact with the API via HTTP requests (using tools like `curl`, Postman, or a frontend application).
2. **REST API (Spring Boot):** Handles CRUD operations and task execution requests.
    - **Task Management:** Create, read, update, delete tasks.
    - **Task Execution:** Execute a task command in a Kubernetes pod.
3. **MongoDB Database:** Stores tasks and task execution logs.
4. **Kubernetes Integration:** Executes commands inside a Kubernetes pod.
5. **Task Execution Logging:** Logs output, start time, and end time.
6. **Command Validation:** Prevents dangerous commands (`rm`, `mkfs`, etc.).


### Key Dependencies in `pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter Data MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>

    <!-- Spring Boot Starter Test (for Unit Tests) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Setup and Installation

### Prerequisites

Ensure the following are installed and configured:

- **Java 17+**: [Download JDK](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
- **Maven**: [Download Maven](https://maven.apache.org/download.cgi)
- **MongoDB**: [Install MongoDB](https://www.mongodb.com/docs/manual/installation/) (default: `mongodb://localhost:27017/taskdb`)
- **Docker**: [Install Docker](https://docs.docker.com/get-docker/)
- **Minikube**: [Install Minikube](https://minikube.sigs.k8s.io/docs/start/)
- **kubectl**: [Install kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Step 1: Set Up Docker and Minikube

1. Ensure Docker is running on your system.

2. Start Minikube using Docker as the driver:

```bash
minikube start --driver=docker
```

3. Verify that Minikube is running:

```bash
minikube status
```

4. Ensure `kubectl` is pointing to the Minikube context:

```bash
kubectl config current-context
```

Expected Output: `minikube`

5. Verify the Kubernetes node status:

```bash
kubectl get nodes
```

Expected Output: Node named `minikube` with `Ready` status.

### Step 2: Clone the Repository

```bash
git clone https://github.com/chaitanyareddy963/java-rest-api.git
cd java-rest-api
```

### Step 3: Build the Application

```bash
mvn clean install
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

Access the API at `http://localhost:8080`.

### Step 5: Verify the Application

Check if the API is running:

```bash
curl http://localhost:8080
```

Expected Output: `Welcome to the Task API`

Alternatively, you can use Postman to verify the application:

1. Open Postman.
2. Create a new GET request: `http://localhost:8080`
3. Click "Send".
4. Ensure the response body displays: `Welcome to the Task API`.

## Using the Task API

### Step 1: Create a Task

**Using curl:**

```bash
curl -X PUT -H "Content-Type: application/json" -d '{
  "id": "task-1",
  "name": "List Files Task",
  "owner": "Your Name",
  "command": "ls -l /"
}' http://localhost:8080/tasks/task-1
```

**Using Postman:**

1. Create a new PUT request: `http://localhost:8080/tasks/task-1`
2. Set the request body (raw, JSON):

```json
{
  "id": "task-1",
  "name": "List Files Task",
  "owner": "Your Name",
  "command": "ls -l /"
}
```

3. Click "Send" and confirm the task is created.

### Step 2: Retrieve a Task

**Using curl:**

```bash
curl http://localhost:8080/tasks/task-1
```

**Using Postman:**

1. Create a new GET request: `http://localhost:8080/tasks/task-1`
2. Click "Send" and view the task details.

### Step 3: Execute a Task

**Using curl:**

```bash
curl -X PUT http://localhost:8080/tasks/task-1/executions
```

**Using Postman:**

1. Create a new PUT request: `http://localhost:8080/tasks/task-1/executions`
2. Click "Send" to trigger task execution.

### Step 4: Retrieve All Tasks

**Using curl:**

```bash
curl http://localhost:8080/tasks
```

**Using Postman:**

1. Create a new GET request: `http://localhost:8080/tasks`
2. Click "Send" to fetch all tasks.

### Step 5: Delete a Task

**Using curl:**

```bash
curl -X DELETE http://localhost:8080/tasks/task-1
```

**Using Postman:**

1. Create a new DELETE request: `http://localhost:8080/tasks/task-1`
2. Click "Send" to delete the task.

## Security Considerations

The API validates task commands to prevent potentially harmful operations (e.g., `rm`, `mkfs`, etc.).

## Author

G Chaitanya Reddy
