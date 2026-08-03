package com.taskmanagement.service;

import com.taskmanagement.dto.TaskRequest;
import com.taskmanagement.dto.TaskResponse;
import com.taskmanagement.dto.UserResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Priority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    // ✅ ADD THIS METHOD
    public List<Task> getAllTasks() {
        System.out.println("📋 Getting all tasks");
        return taskRepository.findAll();
    }

    @Transactional
    public Task createTask(TaskRequest request, Long createdBy) {
        System.out.println("Creating task: " + request.getTitle());

        User creator = userService.findById(createdBy);
        User assignedTo = request.getAssignedTo() != null ? userService.findById(request.getAssignedTo()) : null;

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setPriority(request.getPriority() != null ? Priority.valueOf(request.getPriority().toUpperCase())
                : Priority.MEDIUM);
        task.setStatus(request.getStatus() != null ? TaskStatus.valueOf(request.getStatus().toUpperCase())
                : TaskStatus.NOT_STARTED);
        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());
        task.setAssignedTo(assignedTo);
        task.setCreatedBy(creator);
        task.setProgress(0);

        return taskRepository.save(task);
    }

    public List<Task> getTasksForUser(Long userId) {
        User user = userService.findById(userId);
        return taskRepository.findByAssignedTo(user);
    }

    public List<Task> getTasksCreatedBy(Long userId) {
        User user = userService.findById(userId);
        return taskRepository.findByCreatedBy(user);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    @Transactional
    public Task updateTask(Long id, TaskRequest request) {
        Task task = getTaskById(id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());

        if (request.getPriority() != null) {
            task.setPriority(Priority.valueOf(request.getPriority().toUpperCase()));
        }

        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
        }

        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());

        if (request.getAssignedTo() != null) {
            task.setAssignedTo(userService.findById(request.getAssignedTo()));
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTaskStatus(Long id, String status) {
        Task task = getTaskById(id);
        task.setStatus(TaskStatus.valueOf(status.toUpperCase()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTaskProgress(Long id, Integer progress) {
        Task task = getTaskById(id);
        task.setProgress(progress);
        if (progress == 100) {
            task.setStatus(TaskStatus.READY_FOR_SUBMISSION);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
        System.out.println("Task deleted: " + task.getTitle());
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findByDeadlineBeforeAndStatusNot(LocalDateTime.now(), TaskStatus.COMPLETED);
    }

    public TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setCategory(task.getCategory());
        response.setPriority(task.getPriority() != null ? task.getPriority().name() : "MEDIUM");
        response.setStatus(task.getStatus() != null ? task.getStatus().name() : "NOT_STARTED");
        response.setStartDate(task.getStartDate());
        response.setDeadline(task.getDeadline());
        response.setProgress(task.getProgress());
        response.setCreatedAt(task.getCreatedAt());

        if (task.getAssignedTo() != null) {
            UserResponse assigned = new UserResponse();
            assigned.setId(task.getAssignedTo().getId());
            assigned.setUsername(task.getAssignedTo().getUsername());
            assigned.setName(task.getAssignedTo().getName());
            response.setAssignedTo(assigned);
        }
        if (task.getCreatedBy() != null) {
            UserResponse created = new UserResponse();
            created.setId(task.getCreatedBy().getId());
            created.setUsername(task.getCreatedBy().getUsername());
            created.setName(task.getCreatedBy().getName());
            response.setCreatedBy(created);
        }
        return response;
    }

    public List<TaskResponse> mapToResponseList(List<Task> tasks) {
        return tasks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}