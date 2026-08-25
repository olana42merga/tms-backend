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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

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
        } else {
            subTask.setProgress(0);
        }

        if (request.getPriority() != null) {
            try {
                subTask.setPriority(Priority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                subTask.setPriority(Priority.MEDIUM);
            }
        } else {
            subTask.setPriority(Priority.MEDIUM);
        }

        if (request.getStatus() != null) {
            try {
                subTask.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                subTask.setStatus(TaskStatus.NOT_STARTED);
            }
        } else {
            subTask.setStatus(TaskStatus.NOT_STARTED);
        }

        subTask.setDeadline(request.getDeadline());

        SubTask savedSubTask = subTaskRepository.save(subTask);
        log.info("✅ SubTask created with ID: {}", savedSubTask.getId());

        if (assignedTo != null) {
            sendSubTaskCreatedEmail(savedSubTask, assignedTo);
        }

        return savedSubTask;
    }

    private void sendSubTaskCreatedEmail(SubTask subTask, User assignedTo) {
        try {
            if (assignedTo == null || assignedTo.getEmail() == null) {
                log.warn("⚠️ Cannot send email: assignedTo or email is null");
                return;
            }

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
        try {
            List<SubTask> subTasks = subTaskRepository.findAll();
            if (subTasks == null) {
                return new ArrayList<>();
            }
            log.info("✅ Found {} sub-tasks", subTasks.size());
            return subTasks;
        } catch (Exception e) {
            log.error("❌ Error getting all sub-tasks: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public SubTask getSubTaskById(Long id) {
        log.info("📋 Getting sub-task by ID: {}", id);
        return subTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubTask not found: " + id));
    }

    public List<SubTask> getSubTasksByTask(Long taskId) {
        log.info("📋 Getting sub-tasks for task: {}", taskId);
        try {
            if (taskId == null) {
                log.warn("⚠️ TaskId is null");
                return new ArrayList<>();
            }
            List<SubTask> subTasks = subTaskRepository.findByTaskId(taskId);
            if (subTasks == null) {
                return new ArrayList<>();
            }
            log.info("✅ Found {} sub-tasks for task {}", subTasks.size(), taskId);
            return subTasks;
        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks for task: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<SubTask> getSubTasksAssignedToUser(Long userId) {
        log.info("📋 Getting sub-tasks assigned to user: {}", userId);
        try {
            if (userId == null) {
                log.warn("⚠️ UserId is null");
                return new ArrayList<>();
            }
            List<SubTask> subTasks = subTaskRepository.findByAssignedToUserId(userId);
            if (subTasks == null) {
                log.info("📋 No sub-tasks found for user {}", userId);
                return new ArrayList<>();
            }
            log.info("✅ Found {} sub-tasks for user {}", subTasks.size(), userId);
            return subTasks;
        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks assigned to user: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
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
                // keep existing
            }
        }
        if (request.getStatus() != null) {
            try {
                subTask.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // keep existing
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

        if (subTask.getStatus() == TaskStatus.COMPLETED && subTask.getAssignedTo() != null) {
            sendSubTaskCompletedEmail(updatedSubTask);
        }

        return updatedSubTask;
    }

    private void sendSubTaskCompletedEmail(SubTask subTask) {
        try {
            if (subTask.getAssignedTo() != null && subTask.getAssignedTo().getEmail() != null) {
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
        log.info("✅ SubTask deleted: {}", id);
    }

    // ✅ NEW: Submit subtask (finalise) – now sets submittedAt
    @Transactional
    public SubTask submitSubtask(Long id) {
        log.info("📤 Submitting subtask: {}", id);
        SubTask subTask = getSubTaskById(id);
        // Only allow if status is COMPLETED
        if (subTask.getStatus() != TaskStatus.COMPLETED) {
            throw new RuntimeException("Subtask must be completed before submission");
        }
        subTask.setStatus(TaskStatus.SUBMITTED);
        subTask.setSubmittedAt(LocalDateTime.now()); // ✅ store the timestamp
        return subTaskRepository.save(subTask);
    }
}