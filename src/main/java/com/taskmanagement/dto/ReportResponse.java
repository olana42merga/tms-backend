package com.taskmanagement.dto;

import com.taskmanagement.entity.Report;
import com.taskmanagement.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private String title;
    private String description;
    private String content;
    private ReportStatus status;
    private String feedback;
    private Integer progress;
    private Long taskId;
    private String taskTitle;
    private UserResponse submittedBy;
    private UserResponse reportedTo;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public static ReportResponse fromReport(Report report) {
        if (report == null)
            return null;

        return ReportResponse.builder()
                .id(report.getId())
                .title(report.getTitle())
                .description(report.getDescription())
                .content(report.getContent())
                .status(report.getStatus())
                .feedback(report.getFeedback())
                .progress(report.getProgress())
                .taskId(report.getTask() != null ? report.getTask().getId() : null)
                .taskTitle(report.getTask() != null ? report.getTask().getTitle() : null)
                .submittedBy(UserResponse.fromUser(report.getSubmittedBy()))
                .reportedTo(UserResponse.fromUser(report.getReportedTo()))
                .submittedAt(report.getSubmittedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}