package com.skala03.skala_backend.entity.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "keyword_criteria")
@IdClass(KeywordCriteriaId.class)
public class KeywordCriteria {



    @Id
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Id
    @Column(name = "keyword_score")
    private Integer keywordScore;

    @Column(name = "keyword_guideline", nullable = false)
    private String keywordGuideline;
}