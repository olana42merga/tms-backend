package com.taskmanagement.service;

import com.taskmanagement.dto.MeetingRequest;
import com.taskmanagement.entity.Meeting;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.MeetingRepository;
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

    @Transactional
    public Meeting createMeeting(MeetingRequest request, Long createdBy) {
        log.info("📝 Creating meeting: {}", request.getTitle());
        log.info("👤 Created by user ID: {}", createdBy);

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
        return savedMeeting;
    }

    public List<Meeting> getAllMeetings() {
        log.info("📋 Getting all meetings");
        return meetingRepository.findAll();
    }

    public List<Meeting> getMeetingsForUser(Long userId) {
        log.info("📋 Getting meetings for user: {}", userId);
        // Get meetings where the user is a participant
        return meetingRepository.findByParticipantsContaining(String.valueOf(userId));
    }

    public List<Meeting> getMeetingsByDate(LocalDate date) {
        log.info("📋 Getting meetings by date: {}", date);
        return meetingRepository.findByMeetingDate(date);
    }

    public Meeting getMeetingById(Long id) {
        log.info("📋 Getting meeting by ID: {}", id);
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));
    }

    @Transactional
    public Meeting updateMeeting(Long id, MeetingRequest request) {
        log.info("✏️ Updating meeting: {}", id);
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
        if (request.getParticipants() != null)
            meeting.setParticipants(request.getParticipants());

        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting updateMeetingStatus(Long id, String status) {
        log.info("📝 Updating meeting status: {} -> {}", id, status);
        Meeting meeting = getMeetingById(id);
        meeting.setStatus(status);
        return meetingRepository.save(meeting);
    }

    @Transactional
    public void deleteMeeting(Long id) {
        log.info("🗑️ Deleting meeting: {}", id);
        Meeting meeting = getMeetingById(id);
        meetingRepository.delete(meeting);
        log.info("✅ Meeting deleted: {}", meeting.getTitle());
    }
}