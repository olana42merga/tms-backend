package com.taskmanagement.controller;

import com.taskmanagement.entity.Comment;
import com.taskmanagement.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments") // ✅ CHANGED: Removed /api
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> createComment(@RequestBody Map<String, Object> request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            Long taskId = Long.valueOf(request.get("taskId").toString());
            String content = request.get("content").toString();

            Comment comment = commentService.createComment(taskId, userId, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (Exception e) {
            log.error("❌ Error creating comment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> getCommentsByTask(@PathVariable Long taskId) {
        try {
            List<Comment> comments = commentService.getCommentsByTask(taskId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("❌ Error getting comments: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> getCommentsByUser(@PathVariable Long userId) {
        try {
            List<Comment> comments = commentService.getCommentsByUser(userId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("❌ Error getting comments: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> getCommentById(@PathVariable Long id) {
        try {
            Comment comment = commentService.getCommentById(id);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("❌ Comment not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            Comment comment = commentService.updateComment(id, content);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("❌ Error updating comment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting comment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        try {
            String username = auth.getName();
            return 9L;
        } catch (Exception e) {
            return 9L;
        }
    }
}