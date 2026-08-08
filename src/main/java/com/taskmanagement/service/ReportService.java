package com.taskmanagement.service;

import com.taskmanagement.dto.ReportRequest;
import com.taskmanagement.entity.Report;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.ReportStatus;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.ReportRepository;
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
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public List<Report> getAllReports() {
        log.info("📋 Getting all reports");
        try {
            List<Report> reports = reportRepository.findAll();
            if (reports == null) {
                return new ArrayList<>();
            }
            log.info("✅ Found {} reports", reports.size());
            return reports;
        } catch (Exception e) {
            log.error("❌ Error getting all reports: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public Report createReport(ReportRequest request, Long submittedBy) {
        log.info("📝 Creating report: {}", request.getTitle());

        User submitter = userService.findById(submittedBy);
        Task task = request.getTaskId() != null ? taskService.getTaskById(request.getTaskId()) : null;

        Report report = new Report();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress() != null ? request.getProgress() : 0);
        report.setTask(task);
        report.setSubmittedBy(submitter);
        report.setStatus(ReportStatus.PENDING);
        report.setSubmittedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("✅ Report created with ID: {}", savedReport.getId());

        sendReportSubmittedNotifications(savedReport);

        return savedReport;
    }

    private void sendReportSubmittedNotifications(Report report) {
        try {
            // 1. Notify all managers
            List<User> managers = userService.findUsersByRole("MANAGER");
            List<Long> managerIds = new ArrayList<>();
            for (User manager : managers) {
                managerIds.add(manager.getId());
                sendReportEmail(manager, report, "submitted");
            }

            if (!managerIds.isEmpty()) {
                String title = "📊 New Report Submitted: " + report.getTitle();
                String message = report.getSubmittedBy().getName() + " submitted a report with " + report.getProgress()
                        + "% progress";
                notificationService.createNotificationsForUsers(managerIds, title, message, "REPORT");
            }

            // 2. Notify task assignee if different
            if (report.getTask() != null && report.getTask().getAssignedTo() != null) {
                User assignedTo = report.getTask().getAssignedTo();
                if (!assignedTo.getId().equals(report.getSubmittedBy().getId())) {
                    String title = "📊 Report Submitted on Your Task: " + report.getTitle();
                    String message = report.getSubmittedBy().getName() + " submitted a report on your task";
                    notificationService.createNotification(assignedTo.getId(), title, message, "REPORT");
                    sendReportEmail(assignedTo, report, "assigned");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to send report notifications: {}", e.getMessage());
        }
    }

    public List<Report> getReportsByUser(Long userId) {
        log.info("📋 Getting reports by user: {}", userId);
        try {
            if (userId == null) {
                return new ArrayList<>();
            }
            User user = userService.findById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            List<Report> reports = reportRepository.findBySubmittedBy(user);
            return reports != null ? reports : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error getting reports by user: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Report> getReportsByTask(Long taskId) {
        log.info("📋 Getting reports by task: {}", taskId);
        try {
            return reportRepository.findByTaskId(taskId);
        } catch (Exception e) {
            log.error("❌ Error getting reports by task: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Report getReportById(Long id) {
        log.info("📋 Getting report by ID: {}", id);
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    @Transactional
    public Report updateReport(Long id, ReportRequest request) {
        log.info("✏️ Updating report: {}", id);
        Report report = getReportById(id);
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress() != null ? request.getProgress() : 0);
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateReportStatus(Long id, String status, String feedback) {
        log.info("📝 Updating report status: {} -> {}", id, status);
        Report report = getReportById(id);
        ReportStatus oldStatus = report.getStatus();
        report.setStatus(ReportStatus.valueOf(status.toUpperCase()));
        if (feedback != null) {
            report.setFeedback(feedback);
        }
        Report updatedReport = reportRepository.save(report);

        // ✅ Send notifications on status change
        if (report.getSubmittedBy() != null && oldStatus != report.getStatus()) {
            sendReportStatusNotifications(updatedReport);
        }

        return updatedReport;
    }

    private void sendReportStatusNotifications(Report report) {
        try {
            if (report.getSubmittedBy() != null) {
                // 1. Notify the submitter
                String title = "📊 Report " + report.getStatus() + ": " + report.getTitle();
                String message = "Your report has been " + report.getStatus() +
                        (report.getFeedback() != null ? " - Feedback: " + report.getFeedback() : "");
                notificationService.createNotification(report.getSubmittedBy().getId(), title, message, "REPORT");
                sendReportEmail(report.getSubmittedBy(), report, "status");
            }

            // 2. Notify task assignee
            if (report.getTask() != null && report.getTask().getAssignedTo() != null) {
                User assignedTo = report.getTask().getAssignedTo();
                if (report.getSubmittedBy() == null || !assignedTo.getId().equals(report.getSubmittedBy().getId())) {
                    String title = "📊 Report Status Update on Your Task: " + report.getTitle();
                    String message = "Report status changed to: " + report.getStatus();
                    notificationService.createNotification(assignedTo.getId(), title, message, "REPORT");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to send report status notifications: {}", e.getMessage());
        }
    }

    private void sendReportEmail(User recipient, Report report, String type) {
        try {
            if (recipient != null && recipient.getEmail() != null && !recipient.getEmail().isEmpty()) {
                String subject = "Report " + type + ": " + report.getTitle();
                String body = "Hello " + recipient.getName() + ",\n\n" +
                        "A report has been " + type + ":\n" +
                        "📋 Title: " + report.getTitle() + "\n" +
                        "📊 Status: " + report.getStatus() + "\n" +
                        "📝 Description: "
                        + (report.getDescription() != null ? report.getDescription() : "No description") + "\n" +
                        "👤 By: " + report.getSubmittedBy().getName() + "\n" +
                        "📈 Progress: " + report.getProgress() + "%\n" +
                        (report.getFeedback() != null ? "💬 Feedback: " + report.getFeedback() + "\n" : "") +
                        "\nBest regards,\nTMS Team";
                emailService.sendEmail(recipient.getEmail(), subject, body);
                log.info("✅ Report email sent to: {}", recipient.getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send report email: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteReport(Long id) {
        log.info("🗑️ Deleting report: {}", id);
        Report report = getReportById(id);
        reportRepository.delete(report);
        log.info("✅ Report deleted: {}", report.getTitle());
    }

    public List<Report> getPendingReports() {
        log.info("📋 Getting pending reports");
        try {
            return reportRepository.findByStatus(ReportStatus.PENDING);
        } catch (Exception e) {
            log.error("❌ Error getting pending reports: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}