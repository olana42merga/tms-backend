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
    private final EmailService emailService; // ✅ Add EmailService

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

        SubTask savedSubTask = subTaskRepository.save(subTask);
        log.info("✅ SubTask created with ID: {}", savedSubTask.getId());

        // ✅ Send email notification
        if (assignedTo != null) {
            sendSubTaskCreatedEmail(savedSubTask, assignedTo);
        }

        return savedSubTask;
    }

    // ✅ Send email when subtask is created
    private void sendSubTaskCreatedEmail(SubTask subTask, User assignedTo) {
        try {
            String subject = "SubTask Created: " + subTask.getTitle();
            String body = "Hello " + assignedTo.getName() + ",\n\n" +
                    "A new subtask has been created for you:\n" +
                    "📋 Title: " + subTask.getTitle() + "\n" +
                    "📝 Description: "
                    + (subTask.getDescription() != null ? subTask.getDescription() : "No description") + "\n" +
                    "🎯 Priority: " + subTask.getPriority() + "\n" +
                    "📅 Deadline: " + (subTask.getDeadline() != null ? subTask.getDeadline() : "Not set") + "\n\n" +
                    "Best regards,\nTMS Team";

            emailService.sendEmail(assignedTo.getEmail(), subject, body);
            log.info("✅ SubTask creation email sent to: {}", assignedTo.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send subtask creation email: {}", e.getMessage());
        }
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
        SubTask updatedSubTask = subTaskRepository.save(subTask);

        // ✅ Send email when subtask is completed
        if (subTask.getStatus() == TaskStatus.COMPLETED && subTask.getAssignedTo() != null) {
            sendSubTaskCompletedEmail(updatedSubTask);
        }

        return updatedSubTask;
    }

    // ✅ Send email when subtask is completed
    private void sendSubTaskCompletedEmail(SubTask subTask) {
        try {
            if (subTask.getAssignedTo() != null) {
                String subject = "SubTask Completed: " + subTask.getTitle();
                String body = "Hello " + subTask.getAssignedTo().getName() + ",\n\n" +
                        "A subtask has been completed:\n" +
                        "📋 Title: " + subTask.getTitle() + "\n" +
                        "📝 Description: "
                        + (subTask.getDescription() != null ? subTask.getDescription() : "No description") + "\n\n" +
                        "Best regards,\nTMS Team";

                emailService.sendEmail(subTask.getAssignedTo().getEmail(), subject, body);
                log.info("✅ SubTask completion email sent to: {}", subTask.getAssignedTo().getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send subtask completion email: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteSubTask(Long id) {
        log.info("🗑️ Deleting sub-task: {}", id);
        subTaskRepository.deleteById(id);
    }
}