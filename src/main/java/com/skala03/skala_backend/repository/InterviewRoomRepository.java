package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.InterviewRoom;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
@Repository
public interface InterviewRoomRepository extends JpaRepository<InterviewRoom, String>  {

    Optional<InterviewRoom> findById(String roomId);
}
