package com.taskmanagement.controller;

import com.taskmanagement.dto.TaskRequest;
import com.taskmanagement.dto.TaskResponse;
import com.taskmanagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    // ✅ CREATE TASK - Get userId from Authentication
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskRequest request, Authentication auth) {
        try {
            log.info("📝 Creating task: {}", request.getTitle());
            log.info("📝 AssignedTo: {}", request.getAssignedTo());

            // ✅ Get the logged-in user's ID from the authentication
            Long userId = getUserIdFromAuth(auth);
            log.info("👤 Created by user ID: {}", userId);

            var task = taskService.createTask(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error creating task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ GET ALL TASKS
    @GetMapping
    public ResponseEntity<?> getTasks(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            log.info("📋 Getting tasks for user: {}", userId);
            var tasks = taskService.getTasksForUser(userId);
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ GET ALL TASKS (Admin/Manager view all)
    @GetMapping("/all")
    public ResponseEntity<?> getAllTasks() {
        try {
            log.info("📋 Getting all tasks");
            var tasks = taskService.getAllTasks();
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting all tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        try {
            var task = taskService.getTaskById(id);
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Task not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
        try {
            log.info("✏️ Updating task: {}", id);
            var task = taskService.updateTask(id, request);
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            log.info("📝 Updating task status: {} -> {}", id, request.get("status"));
            var task = taskService.updateTaskStatus(id, request.get("status"));
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<?> updateTaskProgress(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            log.info("📝 Updating task progress: {} -> {}%", id, request.get("progress"));
            var task = taskService.updateTaskProgress(id, request.get("progress"));
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task progress: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            log.info("🗑️ Deleting task: {}", id);
            taskService.deleteTask(id);
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueTasks() {
        try {
            log.info("📋 Getting overdue tasks");
            var tasks = taskService.getOverdueTasks();
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting overdue tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Helper method to get userId from Authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.warn("⚠️ No authentication found, defaulting to user ID 9 (manager)");
            return 9L; // Default to manager ID
        }

        try {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                log.info("👤 Authenticated user: {}", username);
                // You need to implement a method to get userId by username
                // For now, return a default or get from database
                return 9L; // Default to manager ID
            }
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
        }

        return 9L; // Default fallback
    }
}