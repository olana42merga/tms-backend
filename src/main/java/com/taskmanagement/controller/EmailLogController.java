package com.taskmanagement.controller;

import com.taskmanagement.dto.EmailLogRequest;
import com.taskmanagement.entity.EmailLog;
import com.taskmanagement.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/email-logs") // ✅ Removed /api
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailLogController {

    private final EmailLogService emailLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllEmailLogs() {
        try {
            List<EmailLog> logs = emailLogService.getAllEmailLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("❌ Error getting email logs: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEmailLogById(@PathVariable Long id) {
        try {
            EmailLog log = emailLogService.getEmailLogById(id);
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            log.error("❌ Email log not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/recipient/{recipient}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEmailLogsByRecipient(@PathVariable String recipient) {
        try {
            List<EmailLog> logs = emailLogService.getEmailLogsByRecipient(recipient);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("❌ Error getting email logs by recipient: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEmailLogsByStatus(@PathVariable String status) {
        try {
            List<EmailLog> logs = emailLogService.getEmailLogsByStatus(status);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("❌ Error getting email logs by status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEmailLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            List<EmailLog> logs = emailLogService.getEmailLogsByDateRange(start, end);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("❌ Error getting email logs by date range: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEmailLogsCount() {
        try {
            Map<String, Long> counts = emailLogService.getEmailLogsCount();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("❌ Error getting email logs count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEmailLog(@RequestBody EmailLogRequest request) {
        try {
            EmailLog log = emailLogService.createEmailLog(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(log);
        } catch (Exception e) {
            log.error("❌ Error creating email log: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEmailLogStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String errorMessage = request.get("errorMessage");
            EmailLog log = emailLogService.updateEmailLogStatus(id, status, errorMessage);
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            log.error("❌ Error updating email log status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEmailLog(@PathVariable Long id) {
        try {
            emailLogService.deleteEmailLog(id);
            return ResponseEntity.ok(Map.of("message", "Email log deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting email log: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}