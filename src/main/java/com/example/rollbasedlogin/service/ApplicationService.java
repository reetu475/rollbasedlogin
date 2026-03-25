package com.example.rollbasedlogin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.rollbasedlogin.model.Application;
import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.repository.ApplicationRepository;
import com.example.rollbasedlogin.repository.JobRepository;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NotificationService notificationService;

    public List<Application> getApplicationsByStudent(String email) {
        return applicationRepository.findByStudentEmail(email);
    }

    public List<Application> getApplicationsByEmployee(String email) {
        return applicationRepository.findByEmployeeJobs(email);
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Application createApplication(Long jobId, String studentEmail, String studentName, 
                                       String coverLetter, MultipartFile resume) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !job.isApproved()) {
            return null; // Job not found or not approved
        }

        Application application = new Application();
        application.setJobId(jobId);
        application.setStudentEmail(studentEmail);
        application.setStudentName(studentName);
        application.setStatus("Applied");
        application.setAppliedDate(java.time.LocalDateTime.now());
        application.setCoverLetter(coverLetter);
        
        // TODO: Handle resume upload
        // For now, just set a placeholder URL
        if (resume != null) {
            application.setResumeUrl("resume/" + resume.getOriginalFilename());
        }

        Application savedApplication = applicationRepository.save(application);
        
        // Notify employee of new application
        notificationService.notifyEmployeeOfNewApplication(savedApplication);
        
        return savedApplication;
    }

    public Application updateApplicationStatus(Long id, String status) {
        Application application = applicationRepository.findById(id).orElse(null);
        if (application != null) {
            application.setStatus(status);
            Application updatedApplication = applicationRepository.save(application);
            
            // Notify student of status change
            notificationService.notifyStudentOfApplicationStatus(updatedApplication);
            
            return updatedApplication;
        }
        return null;
    }
}