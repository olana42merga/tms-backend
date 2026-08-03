package com.taskmanagement.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendEmail(String to, String subject, String body) {
        System.out.println("📧 Email would be sent to: " + to + " - Subject: " + subject);
        System.out.println("📧 Body: " + body);
    }

    public void sendTaskAssignmentEmail(String to, String taskTitle, String deadline) {
        String subject = "📋 New Task Assigned: " + taskTitle;
        String body = "Hello,\n\nYou have been assigned a new task.\n\nTask: " + taskTitle + "\nDeadline: " + deadline + "\n\nPlease login to view the details.\n\nBest regards,\nTMS System";
        sendEmail(to, subject, body);
    }

    public void sendMeetingScheduledEmail(String to, String meetingTitle, String date, String time) {
        String subject = "📅 Meeting Scheduled: " + meetingTitle;
        String body = "Hello,\n\nA meeting has been scheduled.\n\nMeeting: " + meetingTitle + "\nDate: " + date + "\nTime: " + time + "\n\nPlease login to view the details.\n\nBest regards,\nTMS System";
        sendEmail(to, subject, body);
    }

    public void sendWelcomeEmail(String to, String username, String password) {
        String subject = "🎉 Welcome to TMS!";
        String body = "Hello " + username + ",\n\nWelcome to the Task Management System!\n\nYour account has been created.\nUsername: " + username + "\nPassword: " + password + "\n\nPlease login and change your password.\n\nBest regards,\nTMS System";
        sendEmail(to, subject, body);
    }
}