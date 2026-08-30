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
import java.util.stream.Collectors;

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
        emailLog.setRecipientEmail(request.getRecipient());
        emailLog.setSubject(request.getSubject());
        emailLog.setBody(request.getBody());
        emailLog.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        emailLog.setErrorMessage(request.getErrorMessage());

        if ("SENT".equals(emailLog.getStatus())) {
            emailLog.setSentAt(LocalDateTime.now());
        }

        EmailLog saved = emailLogRepository.save(emailLog);
        log.info("✅ Email log saved with ID: {}", saved.getId());
        return saved;
    }

    public List<EmailLog> getAllEmailLogs() {
        log.info("📋 Getting all email logs");
        List<EmailLog> logs = emailLogRepository.findAll();
        log.info("📋 Found {} email logs", logs.size());
        return logs;
    }

    public EmailLog getEmailLogById(Long id) {
        return emailLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email log not found: " + id));
    }

    public List<EmailLog> getEmailLogsByRecipient(String recipient) {
        log.info("📋 Getting email logs for recipient: {}", recipient);
        return emailLogRepository.findAll().stream()
                .filter(log -> recipient.equals(log.getRecipient()))
                .collect(Collectors.toList());
    }

    public List<EmailLog> getEmailLogsByStatus(String status) {
        log.info("📋 Getting email logs with status: {}", status);
        return emailLogRepository.findAll().stream()
                .filter(log -> status.equals(log.getStatus()))
                .collect(Collectors.toList());
    }

    public List<EmailLog> getEmailLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.info("📋 Getting email logs between: {} and {}", start, end);
        return emailLogRepository.findAll().stream()
                .filter(log -> log.getSentAt() != null &&
                        log.getSentAt().isAfter(start) &&
                        log.getSentAt().isBefore(end))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getEmailLogsCount() {
        log.info("📊 Getting email logs count");
        List<EmailLog> all = emailLogRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", (long) all.size());
        counts.put("sent", all.stream().filter(e -> "SENT".equals(e.getStatus())).count());
        counts.put("failed", all.stream().filter(e -> "FAILED".equals(e.getStatus())).count());
        counts.put("pending", all.stream().filter(e -> "PENDING".equals(e.getStatus())).count());
        log.info("📊 Counts: Total={}, Sent={}, Failed={}, Pending={}",
                counts.get("total"), counts.get("sent"), counts.get("failed"), counts.get("pending"));
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
}
