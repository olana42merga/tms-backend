package com.taskmanagement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRequest {
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    private Long assignedTo; // ✅ This is the user ID
}