package com.taskmanagement.controller;

import com.taskmanagement.dto.MeetingRequest;
import com.taskmanagement.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<?> createMeeting(@RequestBody MeetingRequest request, Authentication auth) {
        try {
            log.info("📝 Creating meeting: {}", request.getTitle());

            // ✅ Get userId from authentication
            Long userId = getUserIdFromAuth(auth);
            log.info("👤 Created by user ID: {}", userId);

            var meeting = meetingService.createMeeting(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(meeting);
        } catch (Exception e) {
            log.error("❌ Error creating meeting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllMeetings() {
        try {
            log.info("📋 Getting all meetings");
            var meetings = meetingService.getAllMeetings();
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("❌ Error getting meetings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMeetingById(@PathVariable Long id) {
        try {
            log.info("📋 Getting meeting by ID: {}", id);
            var meeting = meetingService.getMeetingById(id);
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Meeting not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMeetingsForUser(@PathVariable Long userId) {
        try {
            log.info("📋 Getting meetings for user: {}", userId);
            var meetings = meetingService.getMeetingsForUser(userId);
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("❌ Error getting meetings for user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long id, @RequestBody MeetingRequest request) {
        try {
            log.info("✏️ Updating meeting: {}", id);
            var meeting = meetingService.updateMeeting(id, request);
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Error updating meeting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateMeetingStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            log.info("📝 Updating meeting status: {} -> {}", id, request.get("status"));
            var meeting = meetingService.updateMeetingStatus(id, request.get("status"));
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Error updating meeting status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long id) {
        try {
            log.info("🗑️ Deleting meeting: {}", id);
            meetingService.deleteMeeting(id);
            return ResponseEntity.ok(Map.of("message", "Meeting deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting meeting: {}", e.getMessage());
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
            // Get the username from authentication
            String username = auth.getName();
            log.info("👤 Authenticated user: {}", username);

            // Find user by username and get their ID
            // You need to implement this - either through UserService or UserRepository
            // For now, return default manager ID
            // TODO: Implement proper user lookup
            return 9L; // Default to manager ID
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
            return 9L; // Default fallback
        }
    }
}