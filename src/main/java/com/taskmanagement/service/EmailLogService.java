package com.taskmanagement.service;

import com.taskmanagement.dto.EmailLogRequest;
import com.taskmanagement.entity.EmailLog;
import com.taskmanagement.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailLogService {

    private final EmailLogRepository emailLogRepository;

    @Transactional
    public EmailLog createEmailLog(EmailLogRequest request) {
        log.info("📧 Creating email log for: {}", request.getRecipient());

        EmailLog emailLog = new EmailLog();
        emailLog.setRecipient(request.getRecipient());
        emailLog.setSubject(request.getSubject());
        emailLog.setBody(request.getBody());
        emailLog.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        emailLog.setErrorMessage(request.getErrorMessage());

        if ("SENT".equals(emailLog.getStatus())) {
            emailLog.setSentAt(LocalDateTime.now());
        }

        return emailLogRepository.save(emailLog);
    }

    public List<EmailLog> getAllEmailLogs() {
        log.info("📋 Getting all email logs");
        return emailLogRepository.findAll();
    }

    public EmailLog getEmailLogById(Long id) {
        return emailLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email log not found: " + id));
    }

    public List<EmailLog> getEmailLogsByRecipient(String recipient) {
        log.info("📋 Getting email logs for recipient: {}", recipient);
        return emailLogRepository.findByRecipient(recipient);
    }

    public List<EmailLog> getEmailLogsByStatus(String status) {
        log.info("📋 Getting email logs with status: {}", status);
        return emailLogRepository.findByStatus(status);
    }

    public List<EmailLog> getEmailLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.info("📋 Getting email logs between: {} and {}", start, end);
        return emailLogRepository.findBySentAtBetween(start, end);
    }

    public Map<String, Long> getEmailLogsCount() {
        log.info("📊 Getting email logs count");
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", emailLogRepository.count());
        counts.put("sent", emailLogRepository.countSent());
        counts.put("failed", emailLogRepository.countFailed());
        counts.put("pending", emailLogRepository.countPending());
        return counts;
    }

    @Transactional
    public EmailLog updateEmailLogStatus(Long id, String status, String errorMessage) {
        log.info("📝 Updating email log status: {} -> {}", id, status);
        EmailLog emailLog = getEmailLogById(id);
        emailLog.setStatus(status);
        if (errorMessage != null) {
            emailLog.setErrorMessage(errorMessage);
        }
        if ("SENT".equals(status)) {
            emailLog.setSentAt(LocalDateTime.now());
        }
        return emailLogRepository.save(emailLog);
    }

    @Transactional
    public void deleteEmailLog(Long id) {
        log.info("🗑️ Deleting email log: {}", id);
        emailLogRepository.deleteById(id);
    }

    @Transactional
    public EmailLog sendTestEmail(EmailLogRequest request) {
        log.info("📧 Sending test email to: {}", request.getRecipient());

        // Simulate sending email
        try {
            // Here you would integrate with actual email service
            // For now, just log it
            log.info("✅ Test email sent to: {}", request.getRecipient());

            request.setStatus("SENT");
            return createEmailLog(request);
        } catch (Exception e) {
            log.error("❌ Failed to send test email: {}", e.getMessage());
            request.setStatus("FAILED");
            request.setErrorMessage(e.getMessage());
            return createEmailLog(request);
        }
    }
}