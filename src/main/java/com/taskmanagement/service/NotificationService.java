package com.taskmanagement.service;

import com.taskmanagement.entity.Notification;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.NotificationRepository;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; // ✅ used for sending real emails

    // ---------- CREATE FOR SINGLE USER ----------
    @Transactional
    public Notification createNotification(Long userId, String title, String message, String notificationType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setUser(user);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        log.info("📌 Notification created for user: {}", userId);

        // ✅ SEND EMAIL if user has a valid email address
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            try {
                emailService.sendEmail(user.getEmail(), title, message);
                log.info("📧 Email sent to {} for notification {}", user.getEmail(), saved.getId());
            } catch (Exception e) {
                log.error("❌ Failed to send email to {}: {}", user.getEmail(), e.getMessage());
            }
        } else {
            log.warn("⚠️ User {} has no email address – skipping email", userId);
        }

        return saved;
    }

    // ---------- CREATE FOR MULTIPLE USERS (BULK) ----------
    @Transactional
    public void createNotificationsForUsers(List<Long> userIds, String title, String message, String type) {
        List<Notification> notifications = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    Notification notification = new Notification();
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setNotificationType(type);
                    notification.setUser(user);
                    notification.setIsRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notifications.add(notification);

                    // ✅ Send email to each user individually
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        try {
                            emailService.sendEmail(user.getEmail(), title, message);
                            log.info("📧 Email sent to {} (bulk)", user.getEmail());
                        } catch (Exception e) {
                            log.error("❌ Failed to send email to {}: {}", user.getEmail(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed for user {}: {}", userId, e.getMessage());
            }
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("📌 {} notifications created", notifications.size());
        }
    }

    // ---------- QUERY METHODS ----------
    public List<Notification> getNotificationsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = getNotificationById(id);
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unread);
        log.info("📌 {} notifications marked as read", unread.size());
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
