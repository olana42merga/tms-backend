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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subtasks") // ✅ REMOVED /api from here
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final TaskService taskService;
    private final UserService userService;

    @PostConstruct
    public void init() {
        log.info("🚀🚀🚀 SubTaskController IS LOADED! 🚀🚀🚀");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("🔔 PING endpoint called!");
        return ResponseEntity.ok("SubTask controller is alive!");
    }

    @PostMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> createSubTask(@RequestBody SubTaskRequest request, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            Task task = taskService.getTaskById(request.getTaskId());
            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only create SubTasks for tasks assigned to you"));
            }

            log.info("📝 Worker {} creating sub-task: {}", currentUser.getUsername(), request.getTitle());
            SubTask subTask = subTaskService.createSubTask(request, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(subTask);

        } catch (Exception e) {
            log.error("❌ Error creating sub-task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> getMySubTasks(Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            List<SubTask> subTasks = subTaskService.getSubTasksAssignedToUser(currentUser.getId());
            log.info("📋 Worker {} viewing their {} sub-tasks", currentUser.getUsername(), subTasks.size());

            return ResponseEntity.ok(subTasks);

        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> getSubTasksByTask(@PathVariable Long taskId, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            Task task = taskService.getTaskById(taskId);

            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view SubTasks for tasks assigned to you"));
            }

            List<SubTask> subTasks = subTaskService.getSubTasksByTask(taskId);
            log.info("📋 Worker {} viewing {} sub-tasks for task: {}",
                    currentUser.getUsername(), subTasks.size(), task.getTitle());

            return ResponseEntity.ok(subTasks);

        } catch (Exception e) {
            log.error("❌ Error getting sub-tasks for task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> getSubTaskById(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            SubTask subTask = subTaskService.getSubTaskById(id);

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view your own SubTasks"));
            }

            log.info("📋 Worker {} viewing sub-task: {}", currentUser.getUsername(), id);
            return ResponseEntity.ok(subTask);

        } catch (Exception e) {
            log.error("❌ SubTask not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> updateSubTask(@PathVariable Long id, @RequestBody SubTaskRequest request,
            Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            SubTask subTask = subTaskService.getSubTaskById(id);

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own SubTasks"));
            }

            log.info("✏️ Worker {} updating sub-task: {}", currentUser.getUsername(), id);
            SubTask updatedSubTask = subTaskService.updateSubTask(id, request);
            return ResponseEntity.ok(updatedSubTask);

        } catch (Exception e) {
            log.error("❌ Error updating sub-task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> updateSubTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            SubTask subTask = subTaskService.getSubTaskById(id);

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own SubTasks"));
            }

            log.info("📝 Worker {} updating sub-task status: {} -> {}",
                    currentUser.getUsername(), id, request.get("status"));

            SubTask updatedSubTask = subTaskService.updateSubTaskStatus(id, request.get("status"));
            return ResponseEntity.ok(updatedSubTask);

        } catch (Exception e) {
            log.error("❌ Error updating sub-task status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<?> deleteSubTask(@PathVariable Long id, Authentication auth) {
        try {
            String username = auth.getName();
            User currentUser = userService.findByUsername(username);

            SubTask subTask = subTaskService.getSubTaskById(id);

            if (subTask.getAssignedTo() == null || !subTask.getAssignedTo().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own SubTasks"));
            }

            log.info("🗑️ Worker {} deleting sub-task: {}", currentUser.getUsername(), id);
            subTaskService.deleteSubTask(id);
            return ResponseEntity.ok(Map.of("message", "SubTask deleted successfully"));

        } catch (Exception e) {
            log.error("❌ Error deleting sub-task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}