package com.taskmanagement.controller;

import com.taskmanagement.dto.MeetingRequest;
import com.taskmanagement.entity.User;
import com.taskmanagement.service.MeetingService;
import com.taskmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> createMeeting(@RequestBody MeetingRequest request, Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            var meeting = meetingService.createMeeting(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(meeting);
        } catch (Exception e) {
            log.error("❌ Error creating meeting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> getAllMeetings() {
        try {
            var meetings = meetingService.getAllMeetings();
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("❌ Error getting meetings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-meetings")
    @PreAuthorize("hasAnyRole('STAFF')")
    public ResponseEntity<?> getMyMeetings(Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            var meetings = meetingService.getMeetingsForUser(userId);
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("❌ Error getting my meetings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER', 'STAFF')")
    public ResponseEntity<?> getMeetingById(@PathVariable Long id) {
        try {
            var meeting = meetingService.getMeetingById(id);
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Meeting not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> updateMeeting(@PathVariable Long id, @RequestBody MeetingRequest request) {
        try {
            var meeting = meetingService.updateMeeting(id, request);
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Error updating meeting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> updateMeetingStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            var meeting = meetingService.updateMeetingStatus(id, request.get("status"));
            return ResponseEntity.ok(meeting);
        } catch (Exception e) {
            log.error("❌ Error updating meeting status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAMLEADER')")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long id) {
        try {
            meetingService.deleteMeeting(id);
            return ResponseEntity.ok(Map.of("message", "Meeting deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting meeting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        try {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.error("❌ Error getting userId from auth: {}", e.getMessage());
            return null;
        }
    }
}