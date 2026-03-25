package com.example.rollbasedlogin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.rollbasedlogin.model.Job;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByApprovedTrue();
    List<Job> findByApprovedFalse();
    List<Job> findByPostedBy(String postedBy);
    List<Job> findByCompany(String company);
    
    @Query("SELECT j FROM Job j WHERE j.approved = true AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Job> searchJobs(@Param("keyword") String keyword);
}