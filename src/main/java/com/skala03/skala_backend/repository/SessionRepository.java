
package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {


    List<Session> findByRoomIdOrderBySessionDate(String roomId);

    List<Session> findByRoomIdAndSessionStatus(String roomId, Session.SessionStatus status);

    @Query("SELECT s FROM Session s WHERE s.roomId = :roomId AND s.interviewersUserId LIKE %:userId%")
    List<Session> findSessionsByInterviewer(@Param("roomId") String roomId, @Param("userId") String userId);

    @Query("SELECT s FROM Session s WHERE s.interviewersUserId LIKE %:userId%")
    List<Session> findByUserIdInInterviewers(String userId);
}