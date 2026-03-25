package com.example.rollbasedlogin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.rollbasedlogin.model.Application;
import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.model.User;
import com.example.rollbasedlogin.repository.JobRepository;
import com.example.rollbasedlogin.repository.UserRepository;

@Service
public class NotificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;
    public void notifyStudentOfApplicationStatus(Application application) {
        // Get student details
        User student = userRepository.findByEmail(application.getStudentEmail()).orElse(null);
        if (student == null) return;

        String message = String.format(
            "Hello %s, your application for job ID %d has been %s. Please check your dashboard for more details.",
            student.getUsername(),
            application.getJobId(),
            application.getStatus()
        );

        // Send SMS notification (if phone number exists)
        if (student.getPhoneNumber() != null && !student.getPhoneNumber().isEmpty()) {
            sendSMS(student.getPhoneNumber(), message);
        }

        // Send email notification (placeholder - would need email service integration)
        sendEmail(student.getEmail(), "Job Application Status Update", message);
    }

    public void notifyEmployeeOfNewApplication(Application application) {
        // Get job details to find the employee who posted it
        Job job = jobRepository.findById(application.getJobId()).orElse(null);
        if (job != null) {
            User employee = userRepository.findByEmail(job.getPostedBy()).orElse(null);
            if (employee != null) {
                String message = String.format(
                    "Hello %s, a new application has been received for your job posting '%s'. Please review it in your dashboard.",
                    employee.getUsername(),
                    job.getTitle()
                );

                // Send SMS notification
                if (employee.getPhoneNumber() != null && !employee.getPhoneNumber().isEmpty()) {
                    sendSMS(employee.getPhoneNumber(), message);
                }

                // Send email notification
                sendEmail(employee.getEmail(), "New Job Application Received", message);
            }
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        // Twilio SMS implementation would go here
        // For now, just log the message
        System.out.println("SMS to " + phoneNumber + ": " + message);
    }

    private void sendEmail(String email, String subject, String message) {
        // Email service implementation would go here
        // For now, just log the message
        System.out.println("Email to " + email + " - Subject: " + subject + " - Message: " + message);
    }
}