package com.example.rollbasedlogin.controller;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.example.rollbasedlogin.model.Application;
import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.model.User;
import com.example.rollbasedlogin.repository.ApplicationRepository;
import com.example.rollbasedlogin.repository.JobRepository;
import com.example.rollbasedlogin.repository.UserRepository;
import com.example.rollbasedlogin.service.HuggingFaceAIService;
import com.example.rollbasedlogin.service.DocumentParsingService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JobRepository jobRepo;

    @Autowired
    private ApplicationRepository applicationRepo;

    @Autowired
    private HuggingFaceAIService huggingFaceAIService;

    @Autowired
    private DocumentParsingService documentParsingService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobRepo.findAll();
    }

    @GetMapping("/applications")
    public List<Application> getAllApplications() {
        return applicationRepo.findAll();
    }

    @PostMapping("/approve-job/{id}")
    public ResponseEntity<String> approveJob(@PathVariable Long id) {
        Job job = jobRepo.findById(id).orElse(null);
        if (job != null) {
            job.setApproved(true);
            jobRepo.save(job);
            return ResponseEntity.ok("Job approved successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/reject-job/{id}")
    public ResponseEntity<String> rejectJob(@PathVariable Long id) {
        Job job = jobRepo.findById(id).orElse(null);
        if (job != null) {
            jobRepo.delete(job);
            return ResponseEntity.ok("Job rejected successfully");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * AI Resume Ranking Endpoints using Hugging Face
     */

    @PostMapping("/rank-resumes")
    public ResponseEntity<List<HuggingFaceAIService.RankedApplication>> rankResumes(
            @RequestParam Long jobId,
            @RequestParam(required = false) String keywords) {
        try {
            Job job = jobRepo.findById(jobId).orElse(null);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            List<Application> applications = applicationRepo.findByJobId(jobId);
            List<HuggingFaceAIService.RankedApplication> rankedApplications = new ArrayList<>();

            for (Application app : applications) {
                // Read actual resume content from file
                String resumeContent = readResumeContent(app.getResumeUrl());
                
                // Analyze resume content using Hugging Face AI
                HuggingFaceAIService.RankedApplication rankedApp = 
                    huggingFaceAIService.analyzeResumeForJob(resumeContent, job.getDescription(), keywords);
                
                // Set application details
                rankedApp.setApplication(app);
                
                // If Hugging Face analysis failed, use fallback analysis
                if (rankedApp.getCgpa() == null || "N/A".equals(rankedApp.getCgpa()) || 
                    rankedApp.getSkills() == null || "N/A".equals(rankedApp.getSkills())) {
                    HuggingFaceAIService.ResumeAnalysis fallbackAnalysis = huggingFaceAIService.analyzeResume(resumeContent);
                    rankedApp.setCgpa(fallbackAnalysis.getCgpa());
                    rankedApp.setSkills(fallbackAnalysis.getSkills());
                    rankedApp.setEducation(fallbackAnalysis.getEducation());
                }
                
                rankedApplications.add(rankedApp);
            }

            // Sort by score in descending order
            rankedApplications.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
            return ResponseEntity.ok(rankedApplications);
        } catch (Exception e) {
            System.err.println("Error ranking resumes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/rank-resumes/top")
    public ResponseEntity<List<HuggingFaceAIService.RankedApplication>> rankResumesTop(
            @RequestParam Long jobId,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "5") int topN) {
        try {
            ResponseEntity<List<HuggingFaceAIService.RankedApplication>> response = rankResumes(jobId, keywords);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<HuggingFaceAIService.RankedApplication> rankedApplications = response.getBody();
                List<HuggingFaceAIService.RankedApplication> topRanked = 
                    rankedApplications.stream()
                        .limit(topN)
                        .toList();
                return ResponseEntity.ok(topRanked);
            }
            
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error in rankResumesTop: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/rank-resumes/top3")
    public ResponseEntity<List<HuggingFaceAIService.RankedApplication>> getTop3Candidates(
            @RequestParam Long jobId,
            @RequestParam(required = false) String keywords) {
        try {
            return rankResumesTop(jobId, keywords, 3);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Comprehensive job matching using Hugging Face for complete analysis
     */
    @PostMapping("/comprehensive-job-matching/{jobId}")
    public ResponseEntity<List<HuggingFaceAIService.RankedApplication>> comprehensiveJobMatching(
            @PathVariable Long jobId,
            @RequestBody Map<String, String> requestBody) {
        try {
            Job job = jobRepo.findById(jobId).orElse(null);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            String customKeywords = requestBody.get("customKeywords");
            List<Application> applications = applicationRepo.findByJobId(jobId);
            List<HuggingFaceAIService.RankedApplication> rankedApplications = new ArrayList<>();

            for (Application app : applications) {
                // Read actual resume content from file
                String resumeContent = readResumeContent(app.getResumeUrl());
                
                // Analyze resume content using Hugging Face AI
                HuggingFaceAIService.RankedApplication rankedApp = 
                    huggingFaceAIService.analyzeResumeForJob(resumeContent, job.getDescription(), customKeywords);
                
                // Set application details
                rankedApp.setApplication(app);
                
                // If Hugging Face analysis failed, use fallback analysis
                if (rankedApp.getCgpa().equals("N/A") && rankedApp.getSkills().equals("N/A")) {
                    HuggingFaceAIService.ResumeAnalysis fallbackAnalysis = huggingFaceAIService.analyzeResume(resumeContent);
                    rankedApp.setCgpa(fallbackAnalysis.getCgpa());
                    rankedApp.setSkills(fallbackAnalysis.getSkills());
                    rankedApp.setEducation(fallbackAnalysis.getEducation());
                }
                
                rankedApplications.add(rankedApp);
            }

            // Sort by score in descending order
            rankedApplications.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
            return ResponseEntity.ok(rankedApplications);
        } catch (Exception e) {
            System.err.println("Error in comprehensive job matching: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Helper method to read resume content from file using DocumentParsingService
     */
    private String readResumeContent(String resumeUrl) {
        if (resumeUrl == null || resumeUrl.isEmpty()) {
            return "No resume content available";
        }

        try {
            // Handle different resume URL formats
            String filePath = resumeUrl;
            
            // Remove any URL encoding
            filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            
            // Try multiple possible locations for the resume file
            List<String> possiblePaths = new ArrayList<>();
            
            // 1. Try the exact path provided
            possiblePaths.add(filePath);
            
            // 2. If it's a relative path, try with uploads/resumes prefix
            if (!filePath.startsWith("/") && !filePath.startsWith("http") && !filePath.contains(":")) {
                possiblePaths.add("uploads/resumes/" + filePath);
            }
            
            // 3. Try with absolute path from current directory
            possiblePaths.add(System.getProperty("user.dir") + "/" + filePath);
            
            // 4. Try with uploads directory from current directory
            if (!filePath.startsWith("/") && !filePath.startsWith("http") && !filePath.contains(":")) {
                possiblePaths.add(System.getProperty("user.dir") + "/uploads/resumes/" + filePath);
            }
            
            // 5. Try to find the file by name in the uploads/resumes directory
            if (!filePath.startsWith("/") && !filePath.startsWith("http") && !filePath.contains(":")) {
                String fileName = filePath;
                // Extract just the filename if it contains path separators
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                }
                
                // List all files in uploads/resumes and find matching ones
                java.io.File uploadsDir = new java.io.File("uploads/resumes");
                if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                    java.io.File[] files = uploadsDir.listFiles();
                    if (files != null) {
                        for (java.io.File file : files) {
                            if (file.getName().toLowerCase().contains(fileName.toLowerCase()) || 
                                fileName.toLowerCase().contains(file.getName().toLowerCase())) {
                                possiblePaths.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
            
            // Try each possible path using DocumentParsingService
            for (String pathStr : possiblePaths) {
                try {
                    // Use DocumentParsingService to extract text from the file
                    String resumeContent = documentParsingService.extractResumeContent(pathStr);
                    
                    // Check if content was successfully extracted
                    if (resumeContent != null && !resumeContent.trim().isEmpty() && 
                        !resumeContent.contains("Resume file not found") && 
                        !resumeContent.contains("Error reading resume file") &&
                        !resumeContent.equals("")) {
                        System.out.println("Successfully read resume from: " + pathStr);
                        return resumeContent;
                    }
                } catch (Exception e) {
                    // Continue to next path
                    continue;
                }
            }
            
            // If no file found or parsing failed, return error message
            return "Resume file not found or could not be parsed: " + resumeUrl + 
                   "\nTried paths: " + String.join(", ", possiblePaths);
        } catch (Exception e) {
            // Return error message if file reading fails
            return "Error reading resume file: " + e.getMessage();
        }
    }
}
