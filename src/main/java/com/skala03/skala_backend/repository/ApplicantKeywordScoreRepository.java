package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.ApplicantKeywordScore;
import com.skala03.skala_backend.entity.ApplicantKeywordScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicantKeywordScoreRepository extends JpaRepository<ApplicantKeywordScore, ApplicantKeywordScoreId> {
    List<ApplicantKeywordScore> findByApplicantId(String applicantId);

}
