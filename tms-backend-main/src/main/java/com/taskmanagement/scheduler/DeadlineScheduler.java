package com.taskmanagement.scheduler;

import com.taskmanagement.entity.SubTask;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.repository.SubTaskRepository;
import com.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineScheduler {

    private final SubTaskRepository subTaskRepository;
    private final NotificationService notificationService;

    // Runs every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkOverdueSubtasks() {
        log.info("🔍 Checking for overdue subtasks...");
        LocalDateTime now = LocalDateTime.now();

        List<SubTask> overdue = subTaskRepository.findByDeadlineBeforeAndStatusNotIn(
                now,
                List.of(TaskStatus.COMPLETED, TaskStatus.SUBMITTED));

        if (overdue.isEmpty()) {
            log.info("✅ No overdue subtasks found.");
            return;
        }

        for (SubTask subTask : overdue) {
            User assignedUser = subTask.getAssignedTo();
            if (assignedUser == null)
                continue;

            String title = "⏰ Subtask Overdue";
            String message = String.format(
                    "Subtask '%s' (deadline: %s) is overdue. Please complete it as soon as possible.",
                    subTask.getTitle(),
                    subTask.getDeadline());
            notificationService.createNotification(
                    assignedUser.getId(),
                    title,
                    message,
                    "DEADLINE_OVERDUE");
            log.info("📨 Notification sent to user {} for subtask {}", assignedUser.getId(), subTask.getId());
        }
    }
}