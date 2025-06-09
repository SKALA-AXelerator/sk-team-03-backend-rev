package com.skala03.skala_backend.entity;

import jakarta.persistence.*;

// KeywordCriteria
@Entity
@Table(name = "keyword_criteria")
@IdClass(KeywordCriteriaId.class)
public class KeywordCriteria {
    @Id
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Id
    @Column(name = "keyword_score")
    private Integer keywordScore;  // Tinyint는 Integer로 맵핑 (범위 0~255)

    @Column(name = "keyword_guideline", nullable = false)
    private String keywordGuideline;
}
