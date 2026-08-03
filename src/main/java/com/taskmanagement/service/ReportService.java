package com.taskmanagement.service;

import com.taskmanagement.dto.ReportRequest;
import com.taskmanagement.entity.Report;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.ReportStatus;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final TaskService taskService;

    public ReportService(ReportRepository reportRepository, UserService userService, TaskService taskService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.taskService = taskService;
    }

    @Transactional
    public Report createReport(ReportRequest request, Long submittedBy) {
        System.out.println("Creating report: " + request.getTitle());

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

        return reportRepository.save(report);
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
        report.setStatus(ReportStatus.valueOf(status.toUpperCase()));
        if (feedback != null) {
            report.setFeedback(feedback);
        }
        return reportRepository.save(report);
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
        System.out.println("Report deleted: " + report.getTitle());
    }

    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }
}