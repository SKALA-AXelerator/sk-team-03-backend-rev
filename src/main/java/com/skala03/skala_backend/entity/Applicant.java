package com.skala03.skala_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Applicant {

    @Id
    @Column(name = "applicant_id")
    private String applicantId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status", nullable = false)
    private InterviewStatus interviewStatus = InterviewStatus.waiting;  // pending → waiting

    @Column(name = "job_role_id", nullable = false)
    private String jobRoleId;

    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_score")
    private Float totalScore;

    @Column(name = "individual_pdf_path")
    private String individualPdfPath;

    @Column(name = "individual_qna_path")
    private String individualQnaPath;

    @Column(name = "total_comment")
    private String totalComment;

    @Column(name = "next_checkpoint")
    private String nextCheckpoint;
}
