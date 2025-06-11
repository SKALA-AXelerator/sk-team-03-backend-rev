// AdminResultRepository.java
package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminResultRepository extends JpaRepository<Applicant, String> {

    @Query("SELECT a FROM Applicant a " +
            "JOIN FETCH a.jobRole jr " +
            "WHERE jr.jobRoleId = :jobRoleId " +
            "AND a.interviewStatus = 'completed'")
    List<Applicant> findCompletedApplicantsByJobRoleId(@Param("jobRoleId") String jobRoleId);
}