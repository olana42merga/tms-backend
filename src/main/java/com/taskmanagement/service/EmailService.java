package com.taskmanagement.service;

import com.taskmanagement.dto.EmailLogRequest;
import com.taskmanagement.entity.EmailLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogService emailLogService;

    @Async // 🔥 Makes email sending asynchronous – prevents notification endpoint from
           // timing out
    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("📧 Sending email to: {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("obayecha@gmail.com");

            mailSender.send(message);
            log.info("✅ Email sent successfully to: {}", to);

            // Save log as SENT
            saveEmailLog(to, subject, body, "SENT", null);

        } catch (Exception e) {
            log.error("❌ Failed to send email: {}", e.getMessage());
            saveEmailLog(to, subject, body, "FAILED", e.getMessage());
        }
    }

    private void saveEmailLog(String recipient, String subject, String body, String status, String error) {
        try {
            log.info("📝 Saving email log for recipient: {}", recipient);

            EmailLogRequest request = new EmailLogRequest();
            request.setRecipient(recipient);
            request.setSubject(subject);
            request.setBody(body);
            request.setStatus(status);
            request.setErrorMessage(error);

            EmailLog saved = emailLogService.createEmailLog(request);
            log.info("✅ Email log saved with ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("❌ Failed to save email log: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
