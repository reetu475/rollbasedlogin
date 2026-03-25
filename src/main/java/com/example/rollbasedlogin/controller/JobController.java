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

import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.service.JobService;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = jobService.getAllApprovedJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Job>> getAllJobsForAdmin() {
        List<Job> jobs = jobService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/employee")
    public ResponseEntity<List<Job>> getJobsByEmployee(@RequestParam String email) {
        List<Job> jobs = jobService.getJobsByEmployee(email);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        Job createdJob = jobService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
    }

    @PostMapping("/test-pending")
    public ResponseEntity<Job> createTestPendingJob() {
        Job testJob = new Job();
        testJob.setTitle("Test Job - Pending Approval");
        testJob.setCompany("Test Company");
        testJob.setLocation("Test Location");
        testJob.setJobType("Full-time");
        testJob.setRequiredSkills("Java, Spring Boot");
        testJob.setExperienceRequired("2-3 years");
        testJob.setDescription("This is a test job that needs admin approval");
        testJob.setPostedBy("test@example.com");
        testJob.setApproved(false); // Explicitly set to pending
        
        Job createdJob = jobService.createJob(testJob);
        return ResponseEntity.ok(createdJob);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Job>> getPendingJobs() {
        List<Job> jobs = jobService.getPendingJobs();
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Job> approveJob(@PathVariable Long id) {
        Job approvedJob = jobService.approveJob(id);
        return ResponseEntity.ok(approvedJob);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Job> rejectJob(@PathVariable Long id) {
        Job rejectedJob = jobService.rejectJob(id);
        return ResponseEntity.ok(rejectedJob);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(@RequestParam String keyword) {
        List<Job> jobs = jobService.searchJobs(keyword);
        return ResponseEntity.ok(jobs);
    }
}