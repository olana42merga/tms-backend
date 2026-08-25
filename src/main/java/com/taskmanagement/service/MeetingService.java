package com.taskmanagement.service;

import com.taskmanagement.dto.MeetingRequest;
import com.taskmanagement.entity.Meeting;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.MeetingRepository;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailService emailService; // ✅ Add EmailService

    @Transactional
    public Meeting createMeeting(MeetingRequest request, Long createdBy) {
        log.info("📝 Creating meeting: {}", request.getTitle());
        // log.info("");

        User creator = userService.findById(createdBy);

        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setAgenda(request.getAgenda());
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setLocation(request.getLocation());
        meeting.setMeetingLink(request.getMeetingLink());
        meeting.setParticipants(request.getParticipants());
        meeting.setCreatedBy(creator);
        meeting.setStatus("SCHEDULED");

        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("✅ Meeting created with ID: {}", savedMeeting.getId());

        // ✅ Send email invitations to participants
        sendMeetingInvitations(savedMeeting);

        return savedMeeting;
    }

    // ✅ Send meeting invitations
    private void sendMeetingInvitations(Meeting meeting) {
        try {
            String[] participantIds = meeting.getParticipants().split(",");
            for (String id : participantIds) {
                try {
                    User participant = userRepository.findById(Long.parseLong(id.trim())).orElse(null);
                    if (participant != null) {
                        String subject = "Meeting Invitation: " + meeting.getTitle();
                        String body = "Hello " + participant.getName() + ",\n\n" +
                                "You are invited to a meeting:\n" +
                                "📋 Title: " + meeting.getTitle() + "\n" +
                                "📅 Date: " + meeting.getMeetingDate() + "\n" +
                                "🕐 Time: " + meeting.getStartTime() + " - " + meeting.getEndTime() + "\n" +
                                "📍 Location: "
                                + (meeting.getLocation() != null ? meeting.getLocation() : "Not specified") + "\n" +
                                "🔗 Link: "
                                + (meeting.getMeetingLink() != null ? meeting.getMeetingLink() : "Not provided")
                                + "\n\n" +
                                "Best regards,\nTMS Team";

                        emailService.sendEmail(participant.getEmail(), subject, body);
                        log.info("✅ Meeting invitation email sent to: {}", participant.getEmail());
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to send meeting invitation to ID {}: {}", id, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to send meeting invitations: {}", e.getMessage());
        }
    }

    public List<Meeting> getAllMeetings() {
        // log.info("");
        return meetingRepository.findAll();
    }

    public List<Meeting> getMeetingsForUser(Long userId) {
        // log.info("");
        return meetingRepository.findByParticipantsContaining(String.valueOf(userId));
    }

    public List<Meeting> getMeetingsByDate(LocalDate date) {
        // log.info("");
        return meetingRepository.findByMeetingDate(date);
    }

    public Meeting getMeetingById(Long id) {
        // log.info("");
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));
    }

    @Transactional
    public Meeting updateMeeting(Long id, MeetingRequest request) {
        // log.info("");
        Meeting meeting = getMeetingById(id);

        if (request.getTitle() != null)
            meeting.setTitle(request.getTitle());
        if (request.getDescription() != null)
            meeting.setDescription(request.getDescription());
        if (request.getAgenda() != null)
            meeting.setAgenda(request.getAgenda());
        if (request.getMeetingDate() != null)
            meeting.setMeetingDate(request.getMeetingDate());
        if (request.getStartTime() != null)
            meeting.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)
            meeting.setEndTime(request.getEndTime());
        if (request.getLocation() != null)
            meeting.setLocation(request.getLocation());
        if (request.getMeetingLink() != null)
            meeting.setMeetingLink(request.getMeetingLink());
        if (request.getParticipants() != null) {
            meeting.setParticipants(request.getParticipants());
            // Send updated invitations
            sendMeetingInvitations(meeting);
        }

        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting updateMeetingStatus(Long id, String status) {
        // log.info("");
        Meeting meeting = getMeetingById(id);
        meeting.setStatus(status);
        return meetingRepository.save(meeting);
    }

    @Transactional
    public void deleteMeeting(Long id) {
        // log.info("");
        Meeting meeting = getMeetingById(id);
        meetingRepository.delete(meeting);
        log.info("✅ Meeting deleted: {}", meeting.getTitle());
    }
}
