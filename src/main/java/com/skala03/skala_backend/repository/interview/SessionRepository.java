
package com.skala03.skala_backend.repository.interview;

import com.skala03.skala_backend.entity.interview.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {


    List<Session> findByRoomIdOrderBySessionDate(String roomId);

    List<Session> findByRoomIdAndSessionStatus(String roomId, Session.SessionStatus status);

    @Query("SELECT s FROM Session s WHERE s.roomId = :roomId AND s.interviewersUserId LIKE %:userId%")
    List<Session> findSessionsByInterviewer(@Param("roomId") String roomId, @Param("userId") String userId);

    @Query("SELECT s FROM Session s WHERE s.interviewersUserId LIKE %:userId%")
    List<Session> findByUserIdInInterviewers(String userId);

    List<Session> findByRoomId(String roomId);

   /* @Query("SELECT s.sessionId FROM Session s WHERE s.roomId = :roomId AND s.interviewersUserId LIKE %:userId% AND s.sessionStatus IN ('IN_PROGRESS', 'WAITING', 'SCHEDULED') ORDER BY s.sessionStatus DESC, s.sessionDate ASC")
    Optional<Integer> findCurrentSessionIdByRoomAndUser(@Param("roomId") String roomId, @Param("userId") String userId); */

   @Query(value = "SELECT s.session_id FROM sessions s WHERE s.room_id = :roomId AND s.interviewers_user_id LIKE %:userId% AND s.session_status = 'WAITING' ORDER BY s.session_date ASC LIMIT 1", nativeQuery = true)
   Optional<Integer> findCurrentSessionIdByRoomAndUser(@Param("roomId") String roomId, @Param("userId") String userId);

}