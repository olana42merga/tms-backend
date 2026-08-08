package com.taskmanagement.controller;

import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.dto.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Role;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users") // ✅ Added /api
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAllUsers() {
        log.info("📋 GET /users - Fetching all users");
        try {
            List<UserResponse> users = userService.getAllUsers();
            log.info("✅ Found {} users", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            var user = userService.findById(id);
            return ResponseEntity.ok(UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .isActive(user.getIsActive())
                    .build());
        } catch (Exception e) {
            log.error("❌ User not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request) {
        log.info("========================================");
        log.info("📝 POST /users - Admin creating user");
        log.info("📝 Username: {}", request.getUsername());
        log.info("📝 Email: {}", request.getEmail());
        log.info("📝 Role received: '{}'", request.getRole());
        log.info("========================================");

        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("❌ Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already exists"));
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("❌ Email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already exists"));
            }

            String roleStr = request.getRole();
            if (roleStr == null || roleStr.trim().isEmpty()) {
                log.warn("⚠️ No role provided, defaulting to WORKER");
                request.setRole("WORKER");
            } else {
                try {
                    String upperRole = roleStr.trim().toUpperCase();
                    Role.valueOf(upperRole);
                    request.setRole(upperRole);
                    log.info("✅ Valid role: {}", upperRole);
                } catch (IllegalArgumentException e) {
                    log.error("❌ Invalid role: {}", roleStr);
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid role. Must be ADMIN, MANAGER, or WORKER"));
                }
            }

            User user = userService.registerUser(request);
            log.info("✅ User created successfully by admin: {} with role: {}", user.getUsername(), user.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User created successfully",
                    "username", user.getUsername(),
                    "role", user.getRole().name(),
                    "id", user.getId()));
        } catch (Exception e) {
            log.error("❌ Admin create user error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody RegisterRequest request) {
        log.info("========================================");
        log.info("✏️ PUT /users/{} - Admin updating user", id);
        log.info("📝 New role received: '{}'", request.getRole());
        log.info("========================================");

        try {
            var user = userService.updateUser(id, request);
            log.info("✅ User updated: {} with role: {}", user.getUsername(), user.getRole());
            return ResponseEntity.ok(Map.of(
                    "message", "User updated successfully",
                    "username", user.getUsername(),
                    "role", user.getRole().name()));
        } catch (Exception e) {
            log.error("❌ Update user error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            log.info("✅ User deleted: {}", id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Delete user error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            var user = userService.toggleUserStatus(id);
            log.info("✅ User status toggled: {} -> {}", user.getUsername(), user.getIsActive());
            return ResponseEntity.ok(Map.of(
                    "message", "User status updated",
                    "isActive", user.getIsActive()));
        } catch (Exception e) {
            log.error("❌ Toggle status error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}