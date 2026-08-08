package com.taskmanagement.service;

import com.taskmanagement.entity.Comment;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // ✅ NEW: Get all comments (used in CommentController)
    public List<Comment> getAllComments() {
        log.info("📋 Getting all comments");
        return commentRepository.findAll();
    }

    @Transactional
    public Comment createComment(Long taskId, Long userId, String content) {
        log.info("📝 Creating comment for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("✅ Comment created with ID: {}", savedComment.getId());

        // ✅ Send notifications to all relevant users
        sendCommentNotifications(task, user, content);

        return savedComment;
    }

    private void sendCommentNotifications(Task task, User commenter, String content) {
        try {
            List<Long> userIds = new ArrayList<>();

            // 1. Notify assigned user
            if (task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(commenter.getId())) {
                userIds.add(task.getAssignedTo().getId());
                sendCommentEmail(task.getAssignedTo(), task, commenter, content);
            }

            // 2. Notify creator
            if (task.getCreatedBy() != null && !task.getCreatedBy().getId().equals(commenter.getId())) {
                if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(task.getCreatedBy().getId())) {
                    userIds.add(task.getCreatedBy().getId());
                    sendCommentEmail(task.getCreatedBy(), task, commenter, content);
                }
            }

            // 3. Notify all previous commenters
            List<Comment> existingComments = commentRepository.findByTaskId(task.getId());
            for (Comment c : existingComments) {
                Long cId = c.getUser().getId();
                if (!cId.equals(commenter.getId()) && !userIds.contains(cId)) {
                    userIds.add(cId);
                    sendCommentEmail(c.getUser(), task, commenter, content);
                }
            }

            // 4. Create in-app notifications
            if (!userIds.isEmpty()) {
                String title = "💬 New Comment on: " + task.getTitle();
                String message = commenter.getName() + ": "
                        + (content.length() > 50 ? content.substring(0, 50) + "..." : content);
                notificationService.createNotificationsForUsers(userIds, title, message, "COMMENT");
                log.info("✅ Notifications sent to {} users", userIds.size());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send comment notifications: {}", e.getMessage());
        }
    }

    private void sendCommentEmail(User recipient, Task task, User commenter, String content) {
        try {
            if (recipient != null && recipient.getEmail() != null && !recipient.getEmail().isEmpty()) {
                String subject = "New Comment on Task: " + task.getTitle();
                String body = "Hello " + recipient.getName() + ",\n\n" +
                        "A new comment has been added to your task:\n" +
                        "📋 Task: " + task.getTitle() + "\n" +
                        "💬 Comment: " + content + "\n" +
                        "👤 By: " + commenter.getName() + "\n\n" +
                        "Best regards,\nTMS Team";
                emailService.sendEmail(recipient.getEmail(), subject, body);
                log.info("✅ Comment email sent to: {}", recipient.getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send email: {}", e.getMessage());
        }
    }

    public List<Comment> getCommentsByTask(Long taskId) {
        log.info("📋 Getting comments for task: {}", taskId);
        return commentRepository.findByTaskId(taskId);
    }

    public List<Comment> getCommentsByUser(Long userId) {
        log.info("📋 Getting comments by user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return commentRepository.findByUser(user);
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
    }

    @Transactional
    public Comment updateComment(Long id, String content) {
        log.info("✏️ Updating comment: {}", id);
        Comment comment = getCommentById(id);
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long id) {
        log.info("🗑️ Deleting comment: {}", id);
        commentRepository.deleteById(id);
    }
}