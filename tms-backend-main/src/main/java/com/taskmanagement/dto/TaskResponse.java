package com.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    private Integer progress;
    private UserResponse assignedTo;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
}
