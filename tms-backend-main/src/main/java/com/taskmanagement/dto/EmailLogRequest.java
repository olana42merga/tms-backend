package com.taskmanagement.dto;

import lombok.Data;

@Data
public class EmailLogRequest {
    private String recipient;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String template;
    private String content;
    private String body;
    private String status;
    private String errorMessage;
}
