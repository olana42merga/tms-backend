package com.taskmanagement.service;

import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.dto.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Role;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // ✅ Inject EmailService for notifications

    public User registerUser(RegisterRequest request) {
        log.info("========================================");
        log.info("📝 Registering user: {}", request.getUsername());
        log.info("📝 Role from request: '{}'", request.getRole());
        log.info("📝 Role is null? {}", request.getRole() == null);
        log.info("📝 Role class: {}", request.getRole() != null ? request.getRole().getClass().getName() : "null");
        log.info("========================================");

        // ✅ FIX: Properly handle the role
        Role role;
        String roleStr = request.getRole();

        if (roleStr == null || roleStr.trim().isEmpty()) {
            log.warn("⚠️ No role provided, defaulting to WORKER");
            role = Role.WORKER;
        } else {
            try {
                // Convert to uppercase and trim
                String upperRole = roleStr.trim().toUpperCase();
                log.info("🔄 Converting '{}' to '{}'", roleStr, upperRole);
                role = Role.valueOf(upperRole);
                log.info("✅ Successfully parsed role: {}", role);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid role value: '{}', defaulting to WORKER", roleStr);
                role = Role.WORKER;
            }
        }

        log.info("📝 Final role to save: {}", role);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null ? request.getName() : request.getUsername())
                .phone(request.getPhone())
                .role(role) // ✅ Use the parsed role
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("✅ User saved with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());
        log.info("========================================");

        // ✅ Send welcome email to the new user
        sendWelcomeEmail(savedUser, request.getPassword());

        return savedUser;
    }

    // ✅ Send welcome email to new user
    private void sendWelcomeEmail(User user, String plainPassword) {
        try {
            String subject = "Welcome to Task Management System!";
            String body = "Hello " + user.getName() + ",\n\n" +
                    "Your account has been created successfully.\n\n" +
                    "📋 Account Details:\n" +
                    "   Username: " + user.getUsername() + "\n" +
                    "   Email: " + user.getEmail() + "\n" +
                    "   Role: " + user.getRole().name() + "\n" +
                    "   Password: " + plainPassword + "\n\n" +
                    "Please login to access the system:\n" +
                    "   Login URL: http://localhost:3000/login\n\n" +
                    "Best regards,\n" +
                    "TMS Team";

            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("✅ Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<UserResponse> getAllUsers() {
        log.info("📋 Fetching all users");
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
    }

    public User updateUser(Long id, RegisterRequest request) {
        log.info("========================================");
        log.info("✏️ Updating user ID: {}", id);
        log.info("📝 New role from request: '{}'", request.getRole());

        User user = findById(id);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());

        // ✅ FIX: Update role if provided
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                String upperRole = request.getRole().trim().toUpperCase();
                Role newRole = Role.valueOf(upperRole);
                user.setRole(newRole);
                log.info("✅ Updated role to: {}", newRole);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid role: {}, keeping existing role", request.getRole());
            }
        } else {
            log.warn("⚠️ No role provided in update request, keeping existing role: {}", user.getRole());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("✅ User updated with role: {}", updatedUser.getRole());
        log.info("========================================");
        return updatedUser;
    }

    public void deleteUser(Long id) {
        log.info("🗑️ Deleting user: {}", id);
        userRepository.deleteById(id);
    }

    public User toggleUserStatus(Long id) {
        User user = findById(id);
        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }
}