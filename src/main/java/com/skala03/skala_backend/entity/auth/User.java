package com.skala03.skala_backend.entity.auth;

import com.skala03.skala_backend.entity.interview.InterviewRoom;
import com.skala03.skala_backend.entity.interview.RoomParticipant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_email", unique = true, nullable = false)
    private String userEmail;

    @Column(name = "user_password", nullable = false)
    private String userPassword;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private Role userRole;

    // 🏠 1:N – 방장으로서의 InterviewRoom
    @OneToMany(mappedBy = "leader", fetch = FetchType.LAZY)
    private Set<InterviewRoom> leaderRooms = new HashSet<>();

    // 👥 1:N – RoomParticipants
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoomParticipant> roomParticipants = new HashSet<>();
    public enum Role {
        ADMIN,
        INTERVIEWER
    }
}