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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; // ✅ Add EmailService

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

        Comment savedComment = commentRepository.save(comment);
        log.info("✅ Comment created with ID: {}", savedComment.getId());

        // ✅ Send email notification to task owner
        sendCommentAddedEmail(task, user, content);

        return savedComment;
    }

    // ✅ Send email when comment is added
    private void sendCommentAddedEmail(Task task, User commenter, String content) {
        try {
            if (task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(commenter.getId())) {
                String subject = "New Comment on Task: " + task.getTitle();
                String body = "Hello " + task.getAssignedTo().getName() + ",\n\n" +
                        "A new comment has been added to your task:\n" +
                        "📋 Task: " + task.getTitle() + "\n" +
                        "💬 Comment: " + content + "\n" +
                        "👤 By: " + commenter.getName() + "\n\n" +
                        "Best regards,\nTMS Team";

                emailService.sendEmail(task.getAssignedTo().getEmail(), subject, body);
                log.info("✅ Comment notification email sent to: {}", task.getAssignedTo().getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send comment notification email: {}", e.getMessage());
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
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long id) {
        log.info("🗑️ Deleting comment: {}", id);
        commentRepository.deleteById(id);
    }
}