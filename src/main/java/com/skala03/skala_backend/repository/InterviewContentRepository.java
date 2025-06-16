package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.InterviewContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewContentRepository extends JpaRepository<InterviewContent, String> {

    /**
     * 여러 지원자 ID로 면접 내용 조회 (applicantId 기준)
     */
    @Query("""
        SELECT ic FROM InterviewContent ic 
        JOIN FETCH ic.applicant a
        WHERE a.applicantId IN :applicantIds
        """)
    List<InterviewContent> findByApplicantIds(@Param("applicantIds") List<String> applicantIds);

    /**
     * 단일 지원자 ID로 면접 내용 조회
     */
    @Query("""
        SELECT ic FROM InterviewContent ic 
        JOIN FETCH ic.applicant a
        WHERE a.applicantId = :applicantId
        """)
    List<InterviewContent> findByApplicantId(@Param("applicantId") String applicantId);
}