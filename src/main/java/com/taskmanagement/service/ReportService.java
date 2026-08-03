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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final EmailService emailService; // ✅ Add EmailService

    @Transactional
    public Report createReport(ReportRequest request, Long submittedBy) {
        log.info("📝 Creating report: {}", request.getTitle());

        User submitter = userService.findById(submittedBy);
        Task task = request.getTaskId() != null ? taskService.getTaskById(request.getTaskId()) : null;

        Report report = new Report();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress());
        report.setTask(task);
        report.setSubmittedBy(submitter);
        report.setStatus(ReportStatus.PENDING);

        Report savedReport = reportRepository.save(report);
        log.info("✅ Report created with ID: {}", savedReport.getId());

        // ✅ Send email notification to manager
        sendReportSubmittedEmail(savedReport);

        return savedReport;
    }

    // ✅ Send email when report is submitted
    private void sendReportSubmittedEmail(Report report) {
        try {
            // Notify managers (default manager ID 9)
            User manager = userService.findById(9L);
            if (manager != null && report.getSubmittedBy() != null) {
                String subject = "Report Submitted: " + report.getTitle();
                String body = "Hello " + manager.getName() + ",\n\n" +
                        "A new report has been submitted:\n" +
                        "📋 Title: " + report.getTitle() + "\n" +
                        "📝 Description: "
                        + (report.getDescription() != null ? report.getDescription() : "No description") + "\n" +
                        "👤 Submitted By: " + report.getSubmittedBy().getName() + "\n" +
                        "📊 Progress: " + report.getProgress() + "%\n\n" +
                        "Please review the report.\n\n" +
                        "Best regards,\nTMS Team";

                emailService.sendEmail(manager.getEmail(), subject, body);
                log.info("✅ Report submitted email sent to: {}", manager.getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send report submitted email: {}", e.getMessage());
        }
    }

    public List<Report> getReportsByUser(Long userId) {
        User user = userService.findById(userId);
        return reportRepository.findBySubmittedBy(user);
    }

    public List<Report> getReportsByTask(Long taskId) {
        return reportRepository.findByTaskId(taskId);
    }

    public Report getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    @Transactional
    public Report updateReportStatus(Long id, String status, String feedback) {
        Report report = getReportById(id);
        ReportStatus oldStatus = report.getStatus();
        report.setStatus(ReportStatus.valueOf(status.toUpperCase()));
        if (feedback != null) {
            report.setFeedback(feedback);
        }
        Report updatedReport = reportRepository.save(report);

        // ✅ Send email notification on status change
        if (report.getSubmittedBy() != null && oldStatus != report.getStatus()) {
            sendReportStatusUpdateEmail(updatedReport);
        }

        return updatedReport;
    }

    // ✅ Send email when report status changes
    private void sendReportStatusUpdateEmail(Report report) {
        try {
            if (report.getSubmittedBy() != null) {
                String subject = "Report " + report.getStatus() + ": " + report.getTitle();
                String body = "Hello " + report.getSubmittedBy().getName() + ",\n\n" +
                        "Your report has been " + report.getStatus() + ":\n" +
                        "📋 Title: " + report.getTitle() + "\n" +
                        "📊 Status: " + report.getStatus() + "\n" +
                        "💬 Feedback: " + (report.getFeedback() != null ? report.getFeedback() : "None") + "\n\n" +
                        "Best regards,\nTMS Team";

                emailService.sendEmail(report.getSubmittedBy().getEmail(), subject, body);
                log.info("✅ Report status update email sent to: {}", report.getSubmittedBy().getEmail());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send report status update email: {}", e.getMessage());
        }
    }

    @Transactional
    public Report updateReport(Long id, ReportRequest request) {
        Report report = getReportById(id);
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setContent(request.getContent());
        report.setProgress(request.getProgress());
        return reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Long id) {
        Report report = getReportById(id);
        reportRepository.delete(report);
        log.info("🗑️ Report deleted: {}", report.getTitle());
    }

    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }
}