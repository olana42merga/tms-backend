package com.taskmanagement.controller;

import com.taskmanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test-email") // ✅ REMOVED /api
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TestEmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String body = request.get("body");

            if (to == null || to.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email address is required"));
            }

            log.info("📧 Sending test email to: {}", to);
            emailService.sendEmail(to, subject, body);

            return ResponseEntity.ok(Map.of(
                    "message", "Email sent successfully to " + to,
                    "status", "SENT"));
        } catch (Exception e) {
            log.error("❌ Error sending test email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}