package com.taskmanagement.repository;

import com.taskmanagement.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // ✅ Use @Query to avoid naming issues
    @Query("SELECT e FROM EmailLog e WHERE e.recipient = :recipient")
    List<EmailLog> findByRecipient(String recipient);

    @Query("SELECT e FROM EmailLog e WHERE e.status = :status")
    List<EmailLog> findByStatus(String status);

    @Query("SELECT e FROM EmailLog e WHERE e.sentAt BETWEEN :start AND :end")
    List<EmailLog> findBySentAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = 'SENT'")
    Long countSent();

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = 'FAILED'")
    Long countFailed();

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = 'PENDING'")
    Long countPending();
}