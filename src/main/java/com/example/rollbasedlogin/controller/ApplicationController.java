package com.example.rollbasedlogin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.rollbasedlogin.model.Application;
import com.example.rollbasedlogin.service.ApplicationService;
import com.example.rollbasedlogin.service.DocumentParsingService;
import com.example.rollbasedlogin.service.HuggingFaceAIService;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private HuggingFaceAIService huggingFaceAIService;

    @Autowired
    private DocumentParsingService documentParsingService;

    @GetMapping("/student")
    public ResponseEntity<List<Application>> getApplicationsByStudent(@RequestParam String email) {
        List<Application> applications = applicationService.getApplicationsByStudent(email);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/employee")
    public ResponseEntity<List<Application>> getApplicationsByEmployee(@RequestParam String email) {
        List<Application> applications = applicationService.getApplicationsByEmployee(email);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Application>> getAllApplications() {
        List<Application> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @PostMapping
    public ResponseEntity<Application> createApplication(
            @RequestParam Long jobId,
            @RequestParam String studentEmail,
            @RequestParam String studentName,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) MultipartFile resume) {
        
        Application application = applicationService.createApplication(jobId, studentEmail, studentName, coverLetter, resume);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Application> updateApplicationStatus(@PathVariable Long id, @RequestBody ApplicationStatusRequest request) {
        Application updatedApplication = applicationService.updateApplicationStatus(id, request.getStatus());
        return ResponseEntity.ok(updatedApplication);
    }

    @PostMapping("/analyze-resume")
    public ResponseEntity<?> analyzeResume(@RequestParam("resume") MultipartFile file) {
        try {
            // Save the file temporarily
            String fileName = file.getOriginalFilename();
            String filePath = "uploads/resumes/temp_" + System.currentTimeMillis() + "_" + fileName;
            
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, file.getBytes());
            
            // Use DocumentParsingService to extract text from the saved file
            String resumeContent = documentParsingService.extractResumeContent(filePath);
            
            // Clean up temporary file
            java.nio.file.Files.deleteIfExists(path);
            
            if (resumeContent == null || resumeContent.trim().isEmpty()) {
                ResumeAnalysisResponse errorResponse = new ResumeAnalysisResponse();
                errorResponse.setSuccess(false);
                errorResponse.setError("Failed to extract text from resume file. Please ensure the file is not empty and is in a supported format (PDF, DOC, DOCX, TXT).");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Analyze resume using Hugging Face AI
            HuggingFaceAIService.ResumeAnalysis analysis = huggingFaceAIService.analyzeResume(resumeContent);
            
            // Create response object
            ResumeAnalysisResponse response = new ResumeAnalysisResponse();
            response.setSuccess(true);
            response.setCgpa(analysis.getCgpa());
            response.setSkills(analysis.getSkills());
            response.setEducation(analysis.getEducation());
            response.setExperience(analysis.getExperience());
            response.setMessage("Resume analyzed successfully by Hugging Face AI");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ResumeAnalysisResponse errorResponse = new ResumeAnalysisResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Failed to analyze resume: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Response class for resume analysis
    public static class ResumeAnalysisResponse {
        private boolean success;
        private String cgpa;
        private String skills;
        private String education;
        private String experience;
        private String message;
        private String error;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getCgpa() { return cgpa; }
        public void setCgpa(String cgpa) { this.cgpa = cgpa; }
        public String getSkills() { return skills; }
        public void setSkills(String skills) { this.skills = skills; }
        public String getEducation() { return education; }
        public void setEducation(String education) { this.education = education; }
        public String getExperience() { return experience; }
        public void setExperience(String experience) { this.experience = experience; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}

class ApplicationStatusRequest {
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}