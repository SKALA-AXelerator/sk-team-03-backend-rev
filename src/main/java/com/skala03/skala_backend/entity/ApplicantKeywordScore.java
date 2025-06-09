package com.skala03.skala_backend.entity;

import jakarta.persistence.*;

// ApplicantKeywordScore
@Entity
@Table(name = "applicant_keyword_scores")
@IdClass(ApplicantKeywordScoreId.class)
public class ApplicantKeywordScore {
    @Id
    @Column(name = "applicant_id")
    private String applicantId;

    @Id
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "applicant_score", nullable = false)
    private Integer applicantScore;

    @Column(name = "score_comment")
    private String scoreComment;
}

