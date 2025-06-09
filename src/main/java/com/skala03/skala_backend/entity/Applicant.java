package com.skala03.skala_backend.entity;

import jakarta.persistence.*;

// Applicant
@Entity
@Table(name = "applicants")
public class Applicant {
    @Id
    @Column(name = "applicant_id", nullable = false)
    private String applicantId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status")
    private InterviewStatus interviewStatus = InterviewStatus.진행전;

    @ManyToOne
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRole jobRole;

    @Column(name = "total_score")
    private Float totalScore;

    @Column(name = "individual_pdf_path", columnDefinition = "TEXT")
    private String individualPdfPath;

    @Column(name = "individual_qna_path", columnDefinition = "TEXT")
    private String individualQnaPath;

    @Column(name = "total_comment", columnDefinition = "TEXT")
    private String totalComment;

    @Column(name = "next_checkpoint", columnDefinition = "TEXT")
    private String nextCheckpoint;

    public enum InterviewStatus {
        진행전, 진행완료, 불참
    }
}