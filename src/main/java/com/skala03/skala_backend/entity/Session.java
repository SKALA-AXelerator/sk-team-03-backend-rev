package com.skala03.skala_backend.entity;

import jakarta.persistence.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "session_location")
    private String sessionLocation;

    @Column(name = "session_time")
    private LocalDateTime sessionTime;

    @Column(name = "interviewers_user_id", length = 100)
    private String interviewersUserId;

    @Column(name = "applicants_user_id", length = 100)
    private String applicantsUserId;

    @Column(name = "raw_data_path", columnDefinition = "TEXT")
    private String rawDataPath;
}