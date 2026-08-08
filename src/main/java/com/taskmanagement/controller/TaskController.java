package com.taskmanagement.controller;

import com.taskmanagement.dto.TaskRequest;
import com.taskmanagement.dto.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    // ✅ CREATE TASK - MANAGER only
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createTask(@RequestBody TaskRequest request, Authentication auth) {
        try {
            log.info("📝 Creating task: {}", request.getTitle());
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            log.info("👤 Created by user ID: {}", userId);

            var task = taskService.createTask(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ GET TASKS FOR CURRENT USER - All authenticated users
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> getTasks(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            log.info("📋 Getting tasks for user: {}", userId);
            var tasks = taskService.getTasksForUser(userId);
            if (tasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch tasks: " + e.getMessage()));
        }
    }

    // ✅ GET ALL TASKS - ADMIN and MANAGER
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAllTasks() {
        try {
            log.info("📋 Getting all tasks");
            List<Task> tasks = taskService.getAllTasks();
            if (tasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            log.info("✅ Found {} tasks", tasks.size());
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting all tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch tasks: " + e.getMessage()));
        }
    }

    // ✅ GET MY TASKS - WORKER only
    @GetMapping("/my-tasks")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> getMyTasks(Authentication auth) {
        try {
            log.info("📋 Getting my tasks");
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                log.error("❌ User ID is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            log.info("👤 User ID: {}", userId);

            var tasks = taskService.getTasksForUser(userId);
            if (tasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            log.info("✅ Found {} tasks", tasks.size());
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting my tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch tasks: " + e.getMessage()));
        }
    }

    // ✅ GET TASK BY ID - All authenticated users
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        try {
            log.info("📋 Getting task by ID: {}", id);
            var task = taskService.getTaskById(id);
            if (task == null) {
                log.warn("⚠️ Task not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error getting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch task: " + e.getMessage()));
        }
    }

    // ✅ UPDATE TASK - MANAGER only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
        try {
            log.info("✏️ Updating task: {}", id);
            var task = taskService.updateTask(id, request);
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update task: " + e.getMessage()));
        }
    }

    // ✅ UPDATE TASK STATUS - MANAGER can update all, WORKER can update own
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'WORKER')")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            // Check if worker is updating their own task
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"))) {
                Long userId = getUserIdFromAuth(auth);
                Task task = taskService.getTaskById(id);
                if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You can only update your own tasks"));
                }
            }

            log.info("📝 Updating task status: {} -> {}", id, request.get("status"));
            var task = taskService.updateTaskStatus(id, request.get("status"));
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update task status: " + e.getMessage()));
        }
    }

    // ✅ UPDATE TASK PROGRESS - WORKER only (own tasks) - FIXED
    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> updateTaskProgress(@PathVariable Long id, @RequestBody Map<String, Integer> request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            Task task = taskService.getTaskById(id); // ✅ Only ONE declaration
            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own tasks"));
            }

            log.info("📝 Updating task progress: {} -> {}%", id, request.get("progress"));
            task = taskService.updateTaskProgress(id, request.get("progress")); // ✅ Reuse the variable
            return ResponseEntity.ok(taskService.mapToResponse(task));
        } catch (Exception e) {
            log.error("❌ Error updating task progress: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update task progress: " + e.getMessage()));
        }
    }

    // ✅ DELETE TASK - MANAGER only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            log.info("🗑️ Deleting task: {}", id);
            taskService.deleteTask(id);
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete task: " + e.getMessage()));
        }
    }

    // ✅ GET OVERDUE TASKS - MANAGER only
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getOverdueTasks() {
        try {
            log.info("📋 Getting overdue tasks");
            var tasks = taskService.getOverdueTasks();
            if (tasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            return ResponseEntity.ok(taskService.mapToResponseList(tasks));
        } catch (Exception e) {
            log.error("❌ Error getting overdue tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch overdue tasks: " + e.getMessage()));
        }
    }

    // ✅ Helper method to get userId from Authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.warn("⚠️ No authentication found");
            return null;
        }

        try {
            String username = auth.getName();
            log.info("👤 Authenticated user: {}", username);

            if (username == null || username.isEmpty()) {
                log.warn("⚠️ Username is null or empty");
                return null;
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                log.error("❌ User not found: {}", username);
                return null;
            }

            log.info("✅ Found user ID: {}", user.getId());
            return user.getId();
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage(), e);
            return null;
        }
    }
}