package com.taskmanagement.repository;

import com.taskmanagement.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByMeetingDate(LocalDate date);

    List<Meeting> findByMeetingDateBetween(LocalDate start, LocalDate end);

    List<Meeting> findByParticipantsContaining(String participant); // ✅ Add this
}