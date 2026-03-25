package com.example.rollbasedlogin.controller;

import java.util.List;

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

import com.example.rollbasedlogin.model.Application;
import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.model.User;
import com.example.rollbasedlogin.repository.ApplicationRepository;
import com.example.rollbasedlogin.repository.JobRepository;
import com.example.rollbasedlogin.repository.UserRepository;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private JobRepository jobRepo;

    @Autowired
    private ApplicationRepository applicationRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/jobs")
    public List<Job> getJobsPostedByEmployee(@RequestParam String email) {
        return jobRepo.findByPostedBy(email);
    }

    @GetMapping("/applications")
    public List<Application> getApplicationsForEmployeeJobs(@RequestParam String email) {
        return applicationRepo.findByEmployeeJobs(email);
    }

    @PostMapping("/post-job")
    public ResponseEntity<Job> postJob(@RequestBody Job job) {
        job.setApproved(false); // Jobs need admin approval
        Job savedJob = jobRepo.save(job);
        return ResponseEntity.ok(savedJob);
    }

    @PostMapping("/update-application/{id}")
    public ResponseEntity<Application> updateApplicationStatus(@PathVariable Long id, @RequestBody ApplicationStatusRequest request) {
        Application application = applicationRepo.findById(id).orElse(null);
        if (application != null) {
            application.setStatus(request.getStatus());
            Application updatedApplication = applicationRepo.save(application);
            return ResponseEntity.ok(updatedApplication);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/employees")
    public List<User> getEmployees() {
        return userRepo.findAll().stream()
            .filter(user -> "EMPLOYEE".equals(user.getRole()))
            .toList();
    }

    static class ApplicationStatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
