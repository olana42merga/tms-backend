package com.taskmanagement.controller;

import com.taskmanagement.dto.ReportRequest;
import com.taskmanagement.entity.Report;
import com.taskmanagement.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody ReportRequest request, Authentication auth) {
        try {
            log.info("📝 Creating report: {}", request.getTitle());

            // ✅ Get userId from authentication
            Long userId = getUserIdFromAuth(auth);
            log.info("👤 Submitted by user ID: {}", userId);

            var report = reportService.createReport(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (Exception e) {
            log.error("❌ Error creating report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getReports(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            log.info("📋 Getting reports for user: {}", userId);
            var reports = reportService.getReportsByUser(userId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting reports: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(@PathVariable Long id) {
        try {
            log.info("📋 Getting report by ID: {}", id);
            var report = reportService.getReportById(id);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("❌ Report not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getReportsByTask(@PathVariable Long taskId) {
        try {
            log.info("📋 Getting reports for task: {}", taskId);
            var reports = reportService.getReportsByTask(taskId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting reports for task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReportsByUser(@PathVariable Long userId) {
        try {
            log.info("📋 Getting reports submitted by user: {}", userId);
            var reports = reportService.getReportsByUser(userId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting reports for user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody ReportRequest request) {
        try {
            log.info("✏️ Updating report: {}", id);
            var report = reportService.updateReport(id, request);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("❌ Error updating report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            log.info("📝 Updating report status: {} -> {}", id, request.get("status"));
            var report = reportService.updateReportStatus(id, request.get("status"), request.get("feedback"));
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("❌ Error updating report status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        try {
            log.info("🗑️ Deleting report: {}", id);
            reportService.deleteReport(id);
            return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReports() {
        try {
            log.info("📋 Getting pending reports");
            var reports = reportService.getPendingReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting pending reports: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Helper method to get userId from Authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.warn("⚠️ No authentication found, defaulting to user ID 9 (manager)");
            return 9L; // Default to manager ID
        }

        try {
            String username = auth.getName();
            log.info("👤 Authenticated user: {}", username);
            // TODO: Find user by username and return ID
            // For now, return default manager ID
            return 9L;
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
            return 9L;
        }
    }
}