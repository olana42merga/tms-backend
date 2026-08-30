package com.taskmanagement.service;

import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.dto.UpdateProfileRequest;
import com.taskmanagement.dto.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.Role;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public User registerUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName() != null ? request.getName() : request.getUsername());
        user.setPhone(request.getPhone());

        Role role;
        String roleStr = request.getRole();
        if (roleStr == null || roleStr.trim().isEmpty()) {
            role = Role.STAFF;
        } else {
            try {
                role = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                role = Role.STAFF;
            }
        }
        user.setRole(role);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("✅ User saved with ID: {}", savedUser.getId());

        sendWelcomeEmail(savedUser, request.getPassword());

        return savedUser;
    }

    private void sendWelcomeEmail(User user, String plainPassword) {
        try {
            String loginLink = frontendUrl + "/login";
            String subject = "Welcome to Task Management System!";
            String body = "Hello " + user.getName() + ",\n\n" +
                    "Your account has been created successfully.\n\n" +
                    "Username: " + user.getUsername() + "\n" +
                    "Email: " + user.getEmail() + "\n" +
                    "Password: " + plainPassword + "\n\n" +
                    "Click the link below to login:\n" +
                    loginLink + "\n\n" +
                    "Best regards,\nTMS Team";
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email: {}", e.getMessage());
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

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<User> findUsersByRole(String role) {
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            return userRepository.findByRole(roleEnum);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
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
                .profileImage(user.getProfileImage())
                .build();
    }

    @Transactional
    public User updateUser(Long id, RegisterRequest request) {
        User user = findById(id);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());

        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                user.setRole(Role.valueOf(request.getRole().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Keep existing role
            }
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User toggleUserStatus(Long id) {
        User user = findById(id);
        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }

    @Transactional
    public void updateUserPassword(User user) {
        if (user == null) {
            throw new RuntimeException("User cannot be null");
        }
        userRepository.save(user);
        log.info("✅ Password updated for user: {}", user.getUsername());
    }

    // ✅ Save user (generic method)
    @Transactional
    public User saveUser(User user) {
        if (user == null) {
            throw new RuntimeException("User cannot be null");
        }
        return userRepository.save(user);
    }

    // ============================================================
    // ✅ PROFILE MANAGEMENT METHODS
    // ============================================================

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileImage(Long userId, String imageBase64) {
        User user = findById(userId);
        user.setProfileImage(imageBase64);
        return userRepository.save(user);
    }

    @Transactional
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("✅ Password updated for user: {}", user.getUsername());
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}