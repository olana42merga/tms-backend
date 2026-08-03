package com.taskmanagement.service;

import com.taskmanagement.dto.SubTaskRequest;
import com.taskmanagement.entity.SubTask;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Priority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.repository.SubTaskRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // ✅ FIXED: Accept createdBy parameter
    @Transactional
    public SubTask createSubTask(SubTaskRequest request, Long createdBy) {
        log.info("📝 Creating sub-task: {}", request.getTitle());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found: " + request.getTaskId()));

        User assignedTo = null;
        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getAssignedTo()));
        }

        // ✅ Get the creator (logged-in worker)
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new RuntimeException("User not found: " + createdBy));

        SubTask subTask = new SubTask();
        subTask.setTitle(request.getTitle());
        subTask.setDescription(request.getDescription());
        subTask.setTask(task);
        subTask.setAssignedTo(assignedTo != null ? assignedTo : creator);
        subTask.setCreatedBy(creator);

        if (request.getProgress() != null) {
            subTask.setProgress(request.getProgress());
        }

        if (request.getPriority() != null) {
            try {
                subTask.setPriority(Priority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                subTask.setPriority(Priority.MEDIUM);
            }
        }

        if (request.getStatus() != null) {
            try {
                subTask.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                subTask.setStatus(TaskStatus.NOT_STARTED);
            }
        }

        subTask.setDeadline(request.getDeadline());

        return subTaskRepository.save(subTask);
    }

    public List<SubTask> getAllSubTasks() {
        log.info("📋 Getting all sub-tasks");
        return subTaskRepository.findAll();
    }

    public SubTask getSubTaskById(Long id) {
        return subTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubTask not found: " + id));
    }

    public List<SubTask> getSubTasksByTask(Long taskId) {
        log.info("📋 Getting sub-tasks for task: {}", taskId);
        return subTaskRepository.findByTaskId(taskId);
    }

    public List<SubTask> getSubTasksAssignedToUser(Long userId) {
        log.info("📋 Getting sub-tasks assigned to user: {}", userId);
        return subTaskRepository.findByAssignedToUserId(userId);
    }

    @Transactional
    public SubTask updateSubTask(Long id, SubTaskRequest request) {
        log.info("✏️ Updating sub-task: {}", id);
        SubTask subTask = getSubTaskById(id);

        if (request.getTitle() != null) {
            subTask.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            subTask.setDescription(request.getDescription());
        }
        if (request.getProgress() != null) {
            subTask.setProgress(request.getProgress());
        }
        if (request.getPriority() != null) {
            try {
                subTask.setPriority(Priority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Keep existing priority
            }
        }
        if (request.getStatus() != null) {
            try {
                subTask.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Keep existing status
            }
        }
        if (request.getDeadline() != null) {
            subTask.setDeadline(request.getDeadline());
        }
        if (request.getAssignedTo() != null) {
            User assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getAssignedTo()));
            subTask.setAssignedTo(assignedTo);
        }

        return subTaskRepository.save(subTask);
    }

    @Transactional
    public SubTask updateSubTaskStatus(Long id, String status) {
        log.info("📝 Updating sub-task status: {} -> {}", id, status);
        SubTask subTask = getSubTaskById(id);
        try {
            subTask.setStatus(TaskStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
        return subTaskRepository.save(subTask);
    }

    @Transactional
    public void deleteSubTask(Long id) {
        log.info("🗑️ Deleting sub-task: {}", id);
        subTaskRepository.deleteById(id);
    }
}