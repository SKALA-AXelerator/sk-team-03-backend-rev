// AdminResultRepository.java
package com.skala03.skala_backend.repository.admin;

import com.skala03.skala_backend.entity.applicant.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminResultRepository extends JpaRepository<Applicant, String> {

    @Query("SELECT a FROM Applicant a " +
            "JOIN FETCH a.jobRole jr " +
            "WHERE jr.jobRoleId = :jobRoleId ")
    List<Applicant> findApplicantsByJobRoleId(@Param("jobRoleId") String jobRoleId);
}