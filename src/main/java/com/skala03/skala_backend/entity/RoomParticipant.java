// ================================
// 1. ENTITY (상태 필드 추가)
// ================================

// RoomParticipant.java
package com.skala03.skala_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RoomParticipantId.class)
public class RoomParticipant {
    @Id
    @Column(name = "room_id")
    private String roomId;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false)
    private ParticipantRole participantRole;

    // ✅ 상태 필드 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_status", nullable = false)
    private ParticipantStatus participantStatus = ParticipantStatus.OFFLINE;

    @Column(name = "last_ping_at")
    private LocalDateTime lastPingAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private InterviewRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public enum ParticipantRole {
        LEADER, MEMBER
    }

    public enum ParticipantStatus {
        OFFLINE, WAITING, IN_PROGRESS
    }

    public void updateStatus(ParticipantStatus status) {
        this.participantStatus = status;
        this.lastPingAt = LocalDateTime.now();
    }
}
