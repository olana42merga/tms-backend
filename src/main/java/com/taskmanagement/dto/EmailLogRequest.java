package com.taskmanagement.dto;

import lombok.Data;

@Data
public class EmailLogRequest {
    private String recipient;
    private String subject;
    private String body;
    private String status;
    private String errorMessage;
}