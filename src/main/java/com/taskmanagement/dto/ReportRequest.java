package com.taskmanagement.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private String title;
    private String description;
    private String content;
    private Integer progress;
    private Long taskId;
    private Long submittedById; // ✅ Add this field
}