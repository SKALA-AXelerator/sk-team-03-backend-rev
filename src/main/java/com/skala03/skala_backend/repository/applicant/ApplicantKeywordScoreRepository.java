package com.skala03.skala_backend.repository.applicant;

import com.skala03.skala_backend.entity.applicant.ApplicantKeywordScore;
import com.skala03.skala_backend.entity.applicant.ApplicantKeywordScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicantKeywordScoreRepository extends JpaRepository<ApplicantKeywordScore, ApplicantKeywordScoreId> {
    List<ApplicantKeywordScore> findByApplicantId(String applicantId);
    // ✅ 새로 추가: selected=true인 키워드만 조회
    @Query("""
        SELECT aks FROM ApplicantKeywordScore aks 
        JOIN Keyword k ON aks.keywordId = k.keywordId 
        JOIN JobRoleKeyword jrk ON k.keywordId = jrk.keywordId 
        JOIN Applicant a ON aks.applicantId = a.applicantId 
        WHERE aks.applicantId = :applicantId 
        AND jrk.jobRoleId = a.jobRole.jobRoleId 
        AND jrk.selected = true
        """)
    List<ApplicantKeywordScore> findByApplicantIdWithSelectedKeywords(@Param("applicantId") String applicantId);
}
