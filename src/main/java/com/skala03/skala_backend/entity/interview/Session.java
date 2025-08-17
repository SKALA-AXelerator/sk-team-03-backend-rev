package com.skala03.skala_backend.entity.interview;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    // 어떤 방에서 진행되는 세션인지
    @Column(name = "room_id", nullable = false)
    private String roomId;

    // 세션명 (1회차, 2회차 등)
    @Column(name = "session_name")
    private String sessionName;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "session_location")
    private String sessionLocation;

    @Column(name = "session_time", nullable = false)
    private LocalDateTime sessionTime;

    // 세션 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus = SessionStatus.SCHEDULED;
    // ex) INT_A001,INT_A002
    @Column(name = "interviewers_user_id", length = 100)
    private String interviewersUserId;
    // ex) A001,A002
    @Column(name = "applicants_user_id", length = 100)
    private String applicantsUserId;

    @Column(name = "raw_data_path", columnDefinition = "TEXT")
    private String rawDataPath;

    // InterviewRoom과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private InterviewRoom room;

    // SessionStatus enum
    public enum SessionStatus {
        SCHEDULED,
        WAITING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}