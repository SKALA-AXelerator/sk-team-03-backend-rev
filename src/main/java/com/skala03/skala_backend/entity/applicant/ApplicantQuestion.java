package com.skala03.skala_backend.entity.applicant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
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