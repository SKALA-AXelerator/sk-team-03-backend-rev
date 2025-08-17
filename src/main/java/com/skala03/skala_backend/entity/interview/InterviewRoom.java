package com.skala03.skala_backend.entity.interview;

import com.skala03.skala_backend.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "interview_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoom {
    @Id
    @Column(name = "room_id")
    private String roomId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "leader_user_id")
    private String leaderUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_user_id", insertable = false, updatable = false)
    private User leader;

    // 1:N 방-세션
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Session> sessions = new HashSet<>();

    // 1:N 방-참여자
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoomParticipant> participants = new HashSet<>();
}