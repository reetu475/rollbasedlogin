package com.example.rollbasedlogin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.rollbasedlogin.model.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentEmail(String studentEmail);
    List<Application> findByJobId(Long jobId);
    List<Application> findByJobIdAndStudentEmail(Long jobId, String studentEmail);
    
    @Query("SELECT a FROM Application a WHERE a.jobId IN " +
           "(SELECT j.id FROM Job j WHERE j.postedBy = :employeeEmail)")
    List<Application> findByEmployeeJobs(@Param("employeeEmail") String employeeEmail);
    
    @Query("SELECT a FROM Application a WHERE a.jobId IN " +
           "(SELECT j.id FROM Job j WHERE j.postedBy = :employeeEmail) " +
           "AND a.status = :status")
    List<Application> findByEmployeeJobsAndStatus(@Param("employeeEmail") String employeeEmail, 
                                                  @Param("status") String status);
}