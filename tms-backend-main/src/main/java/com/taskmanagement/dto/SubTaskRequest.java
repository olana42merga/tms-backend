package com.taskmanagement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubTaskRequest {
    private String title;
    private String description;
    private Long taskId;
    private Long assignedTo;
    private String priority;
    private String status;
    private LocalDateTime deadline;
    private Integer progress;
}
