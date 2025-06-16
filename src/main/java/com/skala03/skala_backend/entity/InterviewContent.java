package com.skala03.skala_backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewContent {

    @Id
    @Column(name = "content_id", nullable = false)
    private String contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @Column(name = "interim_transcript", columnDefinition = "TEXT")
    private String interimTranscript;

    @Column(name = "final_transcript", columnDefinition = "TEXT")
    private String finalTranscript;

    @Column(name = "middle_review_text", columnDefinition = "TEXT")
    private String middleReviewText;
}
