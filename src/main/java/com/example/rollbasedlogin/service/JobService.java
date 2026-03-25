package com.example.rollbasedlogin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.rollbasedlogin.model.Job;
import com.example.rollbasedlogin.repository.JobRepository;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> getAllApprovedJobs() {
        return jobRepository.findByApprovedTrue();
    }

    public List<Job> getJobsByEmployee(String email) {
        return jobRepository.findByPostedBy(email);
    }

    public List<Job> getPendingJobs() {
        return jobRepository.findByApprovedFalse();
    }

    public Job createJob(Job job) {
        job.setApproved(false); // Jobs need admin approval
        return jobRepository.save(job);
    }

    public Job approveJob(Long id) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job != null) {
            job.setApproved(true);
            Job approvedJob = jobRepository.save(job);
            System.out.println("Job approved: " + approvedJob.getId() + " - " + approvedJob.getTitle());
            return approvedJob;
        }
        System.out.println("Job not found for approval: " + id);
        return null;
    }

    public Job rejectJob(Long id) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job != null) {
            System.out.println("Job rejected: " + job.getId() + " - " + job.getTitle());
            jobRepository.delete(job);
            return job;
        }
        System.out.println("Job not found for rejection: " + id);
        return null;
    }

    public List<Job> searchJobs(String keyword) {
        return jobRepository.searchJobs(keyword);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
}
