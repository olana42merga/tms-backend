package com.taskmanagement.controller;

import com.taskmanagement.config.JwtUtil;
import com.taskmanagement.dto.AuthRequest;
import com.taskmanagement.dto.AuthResponse;
import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.PasswordResetToken;
import com.taskmanagement.service.UserService;
import com.taskmanagement.service.EmailService;
import com.taskmanagement.service.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final PasswordResetTokenService passwordResetTokenService;

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        log.info("🔐 Login attempt for user: {}", request.getUsername());

        try {
            User user = userService.findByUsername(request.getUsername());

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("❌ Invalid password for user: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            if (!user.getIsActive()) {
                log.warn("❌ Account disabled for user: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Account is disabled"));
            }

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .profileImage(user.getProfileImage())
                    .build();

            log.info("✅ Login successful for user: {} (Role: {})", user.getUsername(), user.getRole().name());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Public registration attempt for user: {}", request.getUsername());

        try {
            // Check if username already exists
            try {
                userService.findByUsername(request.getUsername());
                log.warn("❌ Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already exists"));
            } catch (Exception e) {
                // Username doesn't exist, continue
            }

            // Check if email already exists
            try {
                userService.findByEmail(request.getEmail());
                log.warn("❌ Email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already exists"));
            } catch (Exception e) {
                // Email doesn't exist, continue
            }

            User user = userService.registerUser(request);
            log.info("✅ User registered successfully: {} (Role: {})", user.getUsername(), user.getRole().name());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "User registered successfully",
                            "username", user.getUsername(),
                            "role", user.getRole().name()));
        } catch (Exception e) {
            log.error("❌ Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ FORGOT PASSWORD - Send reset link to email
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            log.info("📧 Password reset requested for: {}", email);

            // Find user by email
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No account found with this email address"));
            }

            // Generate reset token
            String token = UUID.randomUUID().toString();
            passwordResetTokenService.createToken(user, token);

            // Send reset email
            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            String subject = "🔐 Password Reset Request";
            String body = "Hello " + user.getName() + ",\n\n" +
                    "We received a request to reset your password.\n\n" +
                    "Click the link below to reset your password:\n" +
                    resetLink + "\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "Best regards,\nTMS Team";

            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("✅ Password reset email sent to: {}", email);

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset link sent to your email",
                    "success", true));

        } catch (Exception e) {
            log.error("❌ Password reset error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send reset email. Please try again."));
        }
    }

    // ✅ RESET PASSWORD - Set new password using token
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            log.info("🔐 Password reset attempt with token: {}", token);

            // Validate token
            if (!passwordResetTokenService.isValidToken(token)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or expired reset token"));
            }

            // Get user from token
            User user = passwordResetTokenService.getUserByToken(token);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid reset token"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUserPassword(user);

            // Delete used token
            passwordResetTokenService.deleteToken(token);

            log.info("✅ Password reset successful for user: {}", user.getUsername());

            // Send confirmation email
            String subject = "✅ Password Reset Successful";
            String body = "Hello " + user.getName() + ",\n\n" +
                    "Your password has been successfully reset.\n\n" +
                    "If you did not perform this action, please contact support immediately.\n\n" +
                    "Best regards,\nTMS Team";

            emailService.sendEmail(user.getEmail(), subject, body);

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successful",
                    "success", true));

        } catch (Exception e) {
            log.error("❌ Password reset error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password. Please try again."));
        }
    }
}
