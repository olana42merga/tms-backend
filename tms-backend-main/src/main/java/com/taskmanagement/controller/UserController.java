package com.taskmanagement.controller;

import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.dto.UpdatePasswordRequest;
import com.taskmanagement.dto.UpdateProfileRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // ✅ GET current user profile
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            return ResponseEntity.ok(UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .isActive(user.getIsActive())
                    .profileImage(user.getProfileImage())
                    .build());
        } catch (Exception e) {
            log.error("❌ Error getting current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get profile"));
        }
    }

    // ✅ Get all users (Admin, Manager, TeamLeader only)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> getAllUsers(Authentication auth) {
        log.info("📋 GET /users - Fetching all users");
        try {
            List<UserResponse> users = userService.getAllUsers();

            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");

            if ("TEAMLEADER".equals(role)) {
                users = users.stream()
                        .filter(u -> "MANAGER".equals(u.getRole()) || "STAFF".equals(u.getRole()))
                        .collect(Collectors.toList());
                log.info("✅ TEAMLEADER filtered: showing MANAGER and STAFF users ({} users)", users.size());
            } else {
                log.info("✅ Found {} users", users.size());
            }

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    // ✅ NEW: Get recipients (MANAGER and TEAMLEADER only) - Accessible by all
    // authenticated users
    @GetMapping("/recipients")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getRecipients() {
        log.info("📋 GET /users/recipients - Fetching recipients for reports");
        try {
            List<User> allUsers = userRepository.findAll();

            // Filter only MANAGER and TEAMLEADER
            List<UserResponse> recipients = allUsers.stream()
                    .filter(user -> user.getRole() == Role.MANAGER || user.getRole() == Role.TEAMLEADER)
                    .map(user -> UserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .name(user.getName())
                            .phone(user.getPhone())
                            .role(user.getRole().name())
                            .isActive(user.getIsActive())
                            .profileImage(user.getProfileImage())
                            .build())
                    .collect(Collectors.toList());

            log.info("✅ Found {} recipients (MANAGER/TEAMLEADER)", recipients.size());

            // Log recipients for debugging
            recipients.forEach(recipient -> {
                log.info("👤 Recipient: {} (ID: {}, Role: {})", recipient.getName(), recipient.getId(),
                        recipient.getRole());
            });

            return ResponseEntity.ok(recipients);
        } catch (Exception e) {
            log.error("❌ Error fetching recipients: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recipients: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Authentication auth) {
        try {
            var user = userService.findById(id);

            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");

            if ("TEAMLEADER".equals(role) && "ADMIN".equals(user.getRole().name())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You cannot view ADMIN users"));
            }

            return ResponseEntity.ok(UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .isActive(user.getIsActive())
                    .profileImage(user.getProfileImage())
                    .build());
        } catch (Exception e) {
            log.error("❌ User not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Update profile (name, email, phone)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            User updated = userService.updateProfile(user.getId(), request);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", UserResponse.builder()
                            .id(updated.getId())
                            .username(updated.getUsername())
                            .email(updated.getEmail())
                            .name(updated.getName())
                            .phone(updated.getPhone())
                            .role(updated.getRole().name())
                            .isActive(updated.getIsActive())
                            .profileImage(updated.getProfileImage())
                            .build()));
        } catch (Exception e) {
            log.error("❌ Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Upload profile image
    @PostMapping("/profile/image")
    public ResponseEntity<?> uploadProfileImage(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String imageBase64 = request.get("image");
            if (imageBase64 == null || imageBase64.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image data is required"));
            }

            String username = auth.getName();
            User user = userService.findByUsername(username);
            User updated = userService.updateProfileImage(user.getId(), imageBase64);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile image updated",
                    "profileImage", updated.getProfileImage()));
        } catch (Exception e) {
            log.error("❌ Error uploading profile image: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Update password
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest request, Authentication auth) {
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);

            if (!userService.verifyPassword(user, request.getCurrentPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            userService.updatePassword(user, request.getNewPassword());

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            log.error("❌ Error updating password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already exists"));
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("❌ Email already exists: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email already exists"));
            }

            String roleStr = request.getRole();
            if (roleStr == null || roleStr.trim().isEmpty()) {
                log.warn("⚠️ No role provided, defaulting to STAFF");
                request.setRole("STAFF");
            } else {
                try {
                    String upperRole = roleStr.trim().toUpperCase();
                    Role.valueOf(upperRole);
                    request.setRole(upperRole);
                    log.info("✅ Valid role: {}", upperRole);
                } catch (IllegalArgumentException e) {
                    log.error("❌ Invalid role: {}", roleStr);
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid role. Must be ADMIN, MANAGER, TEAMLEADER, or STAFF"));
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