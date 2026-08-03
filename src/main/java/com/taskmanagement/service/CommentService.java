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

        return commentRepository.save(comment);
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