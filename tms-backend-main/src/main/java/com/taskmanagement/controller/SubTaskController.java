package com.taskmanagement.controller;

import com.taskmanagement.dto.SubTaskRequest;
import com.taskmanagement.entity.SubTask;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.SubTaskService;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subtasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final TaskService taskService;
    private final UserService userService;

    @PostConstruct
    public void init() {
        log.info("🚀 SubTaskController IS LOADED!");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("🔔 PING endpoint called!");
        return ResponseEntity.ok("SubTask controller is alive!");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getAllSubTasks() {
        try {
            log.info("📋 Getting all sub-tasks");
            List<SubTask> subTasks = subTaskService.getAllSubTasks();
            return ResponseEntity.ok(subTasks);
        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sub-tasks: " + e.getMessage()));
        }
    }

    @GetMapping("/my-subtasks")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getMySubTasks(Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            List<SubTask> subTasks = subTaskService.getSubTasksAssignedToUser(currentUser.getId());
            if (subTasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            String role = currentUser.getRole().name();
            log.info("📋 {} {} viewing their {} sub-tasks", role, currentUser.getUsername(), subTasks.size());
            return ResponseEntity.ok(subTasks);

        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sub-tasks: " + e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getSubTasksByTask(@PathVariable Long taskId, Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Task not found"));
            }

            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view SubTasks for tasks assigned to you"));
            }

            List<SubTask> subTasks = subTaskService.getSubTasksByTask(taskId);
            if (subTasks == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            log.info("📋 {} {} viewing {} sub-tasks for task: {}",
                    currentUser.getRole().name(), currentUser.getUsername(), subTasks.size(), task.getTitle());

            return ResponseEntity.ok(subTasks);

        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks for task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getSubTaskById(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            SubTask subTask = subTaskService.getSubTaskById(id);
            if (subTask == null) {
                return ResponseEntity.notFound().build();
            }

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view your own SubTasks"));
            }

            log.info("📋 {} {} viewing sub-task: {}", currentUser.getRole().name(), currentUser.getUsername(), id);
            return ResponseEntity.ok(subTask);

        } catch (Exception e) {
            log.error("❌ SubTask not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> createSubTask(@RequestBody SubTaskRequest request, Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Task task = taskService.getTaskById(request.getTaskId());
            if (task == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Task not found"));
            }

            // ✅ Allow TEAMLEADER to create subtasks for tasks they created or are assigned
            // to
            boolean isAssigned = task.getAssignedTo() != null
                    && task.getAssignedTo().getId().equals(currentUser.getId());
            boolean isCreator = task.getCreatedBy() != null
                    && task.getCreatedBy().getId().equals(currentUser.getId());
            boolean isTeamLeader = currentUser.getRole().name().equals("TEAMLEADER");

            if (!isAssigned && !isCreator && !isTeamLeader) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error",
                                "You can only create SubTasks for tasks assigned to you or created by you"));
            }

            // ✅ If TEAMLEADER and no assignedTo specified, assign to themselves
            if (request.getAssignedTo() == null && isTeamLeader) {
                request.setAssignedTo(currentUser.getId());
            }

            log.info("📝 {} {} creating sub-task: {}", currentUser.getRole().name(), currentUser.getUsername(),
                    request.getTitle());
            SubTask subTask = subTaskService.createSubTask(request, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(subTask);

        } catch (Exception e) {
            log.error("❌ Error creating sub-task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> updateSubTask(@PathVariable Long id, @RequestBody SubTaskRequest request,
            Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            SubTask subTask = subTaskService.getSubTaskById(id);
            if (subTask == null) {
                return ResponseEntity.notFound().build();
            }

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own SubTasks"));
            }

            log.info("✏️ {} {} updating sub-task: {}", currentUser.getRole().name(), currentUser.getUsername(), id);
            SubTask updatedSubTask = subTaskService.updateSubTask(id, request);
            return ResponseEntity.ok(updatedSubTask);

        } catch (Exception e) {
            log.error("❌ Error updating sub-task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> updateSubTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            SubTask subTask = subTaskService.getSubTaskById(id);
            if (subTask == null) {
                return ResponseEntity.notFound().build();
            }

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own SubTasks"));
            }

            log.info("📝 {} {} updating sub-task status: {} -> {}",
                    currentUser.getRole().name(), currentUser.getUsername(), id, request.get("status"));

            SubTask updatedSubTask = subTaskService.updateSubTaskStatus(id, request.get("status"));
            return ResponseEntity.ok(updatedSubTask);

        } catch (Exception e) {
            log.error("❌ Error updating sub-task status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> deleteSubTask(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            SubTask subTask = subTaskService.getSubTaskById(id);
            if (subTask == null) {
                return ResponseEntity.notFound().build();
            }

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own SubTasks"));
            }

            log.info("🗑️ {} {} deleting sub-task: {}", currentUser.getRole().name(), currentUser.getUsername(), id);
            subTaskService.deleteSubTask(id);
            return ResponseEntity.ok(Map.of("message", "SubTask deleted successfully"));

        } catch (Exception e) {
            log.error("❌ Error deleting sub-task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> submitSubtask(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            SubTask subTask = subTaskService.getSubTaskById(id);
            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only submit your own subtasks"));
            }

            SubTask updated = subTaskService.submitSubtask(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Error submitting subtask: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        String username = auth.getName();
        User user = userService.findByUsername(username);
        return user != null ? user.getId() : null;
    }
}