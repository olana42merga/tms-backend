package com.taskmanagement.controller;

import com.taskmanagement.dto.ReportRequest;
import com.taskmanagement.dto.ReportResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.ReportService;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> createReport(@RequestBody ReportRequest request, Authentication auth) {
        try {
            log.info("📝 Creating report: {}", request.getTitle());
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            log.info("👤 Submitted by user ID: {}", userId);

            var report = reportService.createReport(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.fromReport(report));
        } catch (Exception e) {
            log.error("❌ Error creating report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> getAllReports() {
        try {
            log.info("📋 Getting all reports");
            var reports = reportService.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting reports: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-reports")
    @PreAuthorize("hasAnyRole('STAFF')")
    public ResponseEntity<?> getMyReports(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            log.info("📋 Getting my reports for user: {}", userId);
            var reports = reportService.getReportsByUser(userId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting my reports: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Get reports sent to the current user (must come BEFORE /{id})
    @GetMapping("/for-me")
    @PreAuthorize("hasAnyRole('MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> getReportsForMe(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            var reports = reportService.getReportsByRecipient(userId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("❌ Error getting reports for me: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
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

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
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

    // ⚠️ This must come AFTER all specific paths (like /for-me, /pending, /task,
    // /user)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody ReportRequest request) {
        try {
            log.info("✏️ Updating report: {}", id);
            var report = reportService.updateReport(id, request);
            return ResponseEntity.ok(ReportResponse.fromReport(report));
        } catch (Exception e) {
            log.error("❌ Error updating report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
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

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.warn("⚠️ No authentication found");
            return null;
        }

        try {
            String username = auth.getName();
            log.info("👤 Authenticated user: {}", username);

            if (username == null || username.isEmpty()) {
                log.warn("⚠️ Username is null or empty");
                return null;
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                log.error("❌ User not found: {}", username);
                return null;
            }

            log.info("✅ Found user ID: {}", user.getId());
            return user.getId();
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
            return null;
        }
    }
}