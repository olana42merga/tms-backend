package com.taskmanagement.controller;

import com.taskmanagement.config.JwtUtil;
import com.taskmanagement.dto.AuthRequest;
import com.taskmanagement.dto.AuthResponse;
import com.taskmanagement.dto.RegisterRequest;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
                    .build();

            log.info("✅ Login successful for user: {} (Role: {})", user.getUsername(), user.getRole().name());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

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
}