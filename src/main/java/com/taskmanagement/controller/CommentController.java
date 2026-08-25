package com.taskmanagement.controller;

import com.taskmanagement.entity.Comment;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.CommentService;
import com.taskmanagement.service.UserService;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getAllComments(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            List<Comment> comments = commentService.getAllComments();
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("❌ Error getting comments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch comments: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getCommentById(@PathVariable Long id) {
        try {
            Comment comment = commentService.getCommentById(id);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("❌ Comment not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getCommentsByTask(@PathVariable Long taskId) {
        try {
            List<Comment> comments = commentService.getCommentsByTask(taskId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("❌ Error getting comments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch comments: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> createComment(@RequestBody Map<String, Object> request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Long taskId = Long.valueOf(request.get("taskId").toString());
            String content = request.get("content").toString();

            Comment comment = commentService.createComment(taskId, userId, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (Exception e) {
            log.error("❌ Error creating comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Comment existingComment = commentService.getCommentById(id);
            if (!existingComment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own comments"));
            }

            String content = request.get("content");
            Comment comment = commentService.updateComment(id, content);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("❌ Error updating comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Comment existingComment = commentService.getCommentById(id);
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!existingComment.getUser().getId().equals(userId) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own comments"));
            }

            commentService.deleteComment(id);
            return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting comment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
            return null;
        }
    }
}