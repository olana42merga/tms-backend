package com.taskmanagement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MeetingRequest {
    private String title;
    private String description;
    private String agenda;
    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String meetingLink;
    private String participants;
    private String status;
}