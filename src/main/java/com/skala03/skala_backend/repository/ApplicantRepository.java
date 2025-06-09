package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, String> {

    @Query("SELECT a FROM Applicant a WHERE a.applicantId IN :applicantIds")
    List<Applicant> findByApplicantIdIn(@Param("applicantIds") List<String> applicantIds);

    @Query("SELECT a FROM Applicant a WHERE a.sessionId = :sessionId")
    List<Applicant> findBySessionId(@Param("sessionId") Integer sessionId);

    @Query("SELECT a FROM Applicant a WHERE a.jobRoleId = :jobRoleId")
    List<Applicant> findByJobRoleId(@Param("jobRoleId") String jobRoleId);
}