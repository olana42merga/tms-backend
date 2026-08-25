package com.taskmanagement.service;

import com.taskmanagement.dto.ReportRequest;
import com.taskmanagement.dto.ReportResponse;
import com.taskmanagement.entity.Report;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.ReportStatus;
import com.taskmanagement.enums.Role;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public List<ReportResponse> getAllReports() {
        log.info("📋 Getting all reports");
        try {
            List<Report> reports = reportRepository.findAll();
            if (reports == null) {
                return new ArrayList<>();
            }
            log.info("✅ Found {} reports", reports.size());
            return reports.stream()
                    .map(ReportResponse::fromReport)
                    .collect(Collectors.toList());
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

        // Validate and set reportedTo
        User reportedTo = null;
        if (request.getReportedToId() != null) {
            reportedTo = userService.findById(request.getReportedToId());
            if (reportedTo.getRole() != Role.MANAGER && reportedTo.getRole() != Role.TEAMLEADER) {
                throw new IllegalArgumentException("Recipient must be a MANAGER or TEAMLEADER");
            }
        } else {
            throw new IllegalArgumentException("Please select a recipient (MANAGER or TEAMLEADER)");
        }

        Report report = new Report();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress() != null ? request.getProgress() : 0);
        report.setTask(task);
        report.setSubmittedBy(submitter);
        report.setReportedTo(reportedTo);
        report.setStatus(ReportStatus.PENDING);
        report.setSubmittedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("✅ Report created with ID: {}", savedReport.getId());

        sendReportSubmittedNotifications(savedReport);

        return savedReport;
    }

    private void sendReportSubmittedNotifications(Report report) {
        try {
            if (report.getReportedTo() != null) {
                User recipient = report.getReportedTo();
                String title = "📊 New Report Submitted: " + report.getTitle();
                String message = report.getSubmittedBy().getName() + " submitted a report with " + report.getProgress()
                        + "% progress";
                notificationService.createNotification(recipient.getId(), title, message, "REPORT");
                sendReportEmail(recipient, report, "submitted");
            }

            if (report.getTask() != null && report.getTask().getAssignedTo() != null) {
                User assignedTo = report.getTask().getAssignedTo();
                if (!assignedTo.getId().equals(report.getSubmittedBy().getId()) &&
                        !assignedTo.getId().equals(report.getReportedTo().getId())) {
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

    public List<ReportResponse> getReportsByUser(Long userId) {
        log.info("📋 Getting reports submitted by user: {}", userId);
        try {
            if (userId == null)
                return new ArrayList<>();
            User user = userService.findById(userId);
            if (user == null)
                return new ArrayList<>();
            List<Report> reports = reportRepository.findBySubmittedBy(user);
            return reports != null ? reports.stream()
                    .map(ReportResponse::fromReport)
                    .collect(Collectors.toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error getting reports by user: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<ReportResponse> getReportsByRecipient(Long recipientId) {
        log.info("📋 Getting reports sent to recipient: {}", recipientId);
        try {
            if (recipientId == null)
                return new ArrayList<>();
            User recipient = userService.findById(recipientId);
            if (recipient == null)
                return new ArrayList<>();
            List<Report> reports = reportRepository.findByReportedTo(recipient);
            return reports != null ? reports.stream()
                    .map(ReportResponse::fromReport)
                    .collect(Collectors.toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error getting reports by recipient: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<ReportResponse> getReportsByTask(Long taskId) {
        log.info("📋 Getting reports by task: {}", taskId);
        try {
            List<Report> reports = reportRepository.findByTaskId(taskId);
            return reports != null ? reports.stream()
                    .map(ReportResponse::fromReport)
                    .collect(Collectors.toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error getting reports by task: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public ReportResponse getReportById(Long id) {
        log.info("📋 Getting report by ID: {}", id);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        return ReportResponse.fromReport(report);
    }

    @Transactional
    public Report updateReport(Long id, ReportRequest request) {
        log.info("✏️ Updating report: {}", id);
        Report report = getReportEntityById(id);
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress() != null ? request.getProgress() : 0);
        return reportRepository.save(report);
    }

    @Transactional
    public ReportResponse updateReportStatus(Long id, String status, String feedback) {
        log.info("📝 Updating report status: {} -> {}", id, status);
        Report report = getReportEntityById(id);
        ReportStatus oldStatus = report.getStatus();
        report.setStatus(ReportStatus.valueOf(status.toUpperCase()));
        if (feedback != null) {
            report.setFeedback(feedback);
        }
        Report updatedReport = reportRepository.save(report);

        if (report.getSubmittedBy() != null && oldStatus != report.getStatus()) {
            sendReportStatusNotifications(updatedReport);
        }

        return ReportResponse.fromReport(updatedReport);
    }

    private Report getReportEntityById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    private void sendReportStatusNotifications(Report report) {
        try {
            if (report.getSubmittedBy() != null) {
                String title = "📊 Report " + report.getStatus() + ": " + report.getTitle();
                String message = "Your report has been " + report.getStatus() +
                        (report.getFeedback() != null ? " - Feedback: " + report.getFeedback() : "");
                notificationService.createNotification(report.getSubmittedBy().getId(), title, message, "REPORT");
                sendReportEmail(report.getSubmittedBy(), report, "status");
            }

            if (report.getReportedTo() != null
                    && !report.getReportedTo().getId().equals(report.getSubmittedBy().getId())) {
                String title = "📊 Report Status Update: " + report.getTitle();
                String message = "Status changed to " + report.getStatus() +
                        (report.getFeedback() != null ? " - Feedback: " + report.getFeedback() : "");
                notificationService.createNotification(report.getReportedTo().getId(), title, message, "REPORT");
                sendReportEmail(report.getReportedTo(), report, "status");
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
        Report report = getReportEntityById(id);
        reportRepository.delete(report);
        log.info("✅ Report deleted: {}", report.getTitle());
    }

    public List<ReportResponse> getPendingReports() {
        log.info("📋 Getting pending reports");
        try {
            List<Report> reports = reportRepository.findByStatus(ReportStatus.PENDING);
            return reports != null ? reports.stream()
                    .map(ReportResponse::fromReport)
                    .collect(Collectors.toList()) : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error getting pending reports: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}