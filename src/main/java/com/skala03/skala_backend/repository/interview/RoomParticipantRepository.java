package com.skala03.skala_backend.repository.interview;

// ================================
// 2. REPOSITORY
// ================================

// RoomParticipantRepository.java


import com.skala03.skala_backend.entity.interview.RoomParticipant;
import com.skala03.skala_backend.entity.interview.RoomParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, RoomParticipantId> {

    List<RoomParticipant> findByRoomId(String roomId);

    List<RoomParticipant> findByRoomIdAndParticipantStatus(String roomId, RoomParticipant.ParticipantStatus status);

    Optional<RoomParticipant> findByRoomIdAndUserId(String roomId, String userId);

    @Query("SELECT COUNT(rp) FROM RoomParticipant rp WHERE rp.roomId = :roomId AND rp.participantStatus = :status")
    long countByRoomIdAndStatus(@Param("roomId") String roomId, @Param("status") RoomParticipant.ParticipantStatus status);

    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.roomId = :roomId AND rp.participantRole = :role")
    Optional<RoomParticipant> findByRoomIdAndRole(@Param("roomId") String roomId, @Param("role") RoomParticipant.ParticipantRole role);

    List<RoomParticipant> findByUserId(String userId);
}
