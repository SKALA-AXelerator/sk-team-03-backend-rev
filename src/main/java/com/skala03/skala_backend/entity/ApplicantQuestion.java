package com.skala03.skala_backend.entity;

import jakarta.persistence.*;


// ApplicantQuestion
@Entity
@Table(name = "applicant_questions")
@IdClass(ApplicantQuestionId.class)
public class ApplicantQuestion {
    @Id
    @Column(name = "applicant_id")
    private String applicantId;

    @Id
    @Column(name = "question_id")
    private Integer questionId;
}