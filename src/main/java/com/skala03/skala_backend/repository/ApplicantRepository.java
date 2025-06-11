package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.Applicant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, String> {

    @Query("SELECT a FROM Applicant a WHERE a.applicantId IN :applicantIds")
    List<Applicant> findByApplicantIdIn(@Param("applicantIds") List<String> applicantIds);

    @Query("SELECT a FROM Applicant a WHERE a.sessionId = :sessionId")
    List<Applicant> findBySessionId(@Param("sessionId") Integer sessionId);

    @Query("SELECT a FROM Applicant a WHERE a.jobRole = :jobRoleId")
    List<Applicant> findByJobRoleId(@Param("jobRoleId") String jobRoleId);

    // ===== 지원자별 질문 조회 메서드 추가 =====
    @Query(value = "SELECT q.question_text FROM questions q " +
            "JOIN applicant_questions aq ON q.question_id = aq.question_id " +
            "WHERE aq.applicant_id = :applicantId " +
            "ORDER BY q.question_id", nativeQuery = true)
    List<String> findQuestionsByApplicantId(@Param("applicantId") String applicantId);

    // 최대 세션 ID 조회
    @Query("SELECT COALESCE(MAX(a.sessionId), 0) FROM Applicant a")
    Integer findMaxSessionId();

    // 특정 세션에서 특정 지원자들을 제외한 나머지 조회
    @Query("SELECT a FROM Applicant a WHERE a.sessionId = :sessionId AND a.applicantId NOT IN :excludeApplicantIds")
    List<Applicant> findBySessionIdAndApplicantIdNotIn(
            @Param("sessionId") Integer sessionId,
            @Param("excludeApplicantIds") List<String> excludeApplicantIds
    );

    @Modifying
    @Query(value = "INSERT INTO sessions (session_id, room_id, session_name, session_date, session_location, session_time, session_status, interviewers_user_id, applicants_user_id, raw_data_path) VALUES (?1, ?2, ?3, CURDATE(), '서울 본사 8층', NOW(), 'scheduled', ?4, ?5, ?6)", nativeQuery = true)
    void createNewSession(Integer sessionId, String roomId, String sessionName, String interviewerIds, String applicantIds, String rawDataPath);

    @Query(value = "SELECT user_id FROM room_participants WHERE room_id = :roomId ORDER BY participant_role DESC", nativeQuery = true)
    List<String> findUserIdsByRoomId(@Param("roomId") String roomId);

    @Query(value = "SELECT k.keyword_name, aks.applicant_score, aks.score_comment " +
            "FROM applicant_keyword_scores aks " +
            "JOIN keywords k ON aks.keyword_id = k.keyword_id " +
            "WHERE aks.applicant_id = :applicantId " +
            "ORDER BY k.keyword_id", nativeQuery = true)
    List<Object[]> findKeywordScoresByApplicantId(@Param("applicantId") String applicantId);
    // 세션에 속한 지원자 전체 수
    @Query("SELECT COUNT(a) FROM Applicant a WHERE a.sessionId = :sessionId")
    int countBySessionId(@Param("sessionId") int sessionId);

    // 세션에 속한 대기중 지원자 수
    @Query("SELECT COUNT(a) FROM Applicant a WHERE a.sessionId = :sessionId AND a.interviewStatus = 'waiting'")
    int countWaitingBySessionId(@Param("sessionId") int sessionId);

    /** 모든 지원자 + 연관 엔티티 즉시 로딩 */
    @EntityGraph(attributePaths = {
            "jobRole",
            "session",
            "keywordScores",
            "keywordScores.keyword"
    })
    List<Applicant> findAllByOrderByCompletedAtDesc();   // 가장 최근 결과가 먼저

    // Repository에 추가할 메서드 (N+1 문제 방지를 위한 fetch join)
// ApplicantRepository.java
    @Query("SELECT a FROM Applicant a JOIN FETCH a.jobRole WHERE a.applicantId IN :applicantIds")
    List<Applicant> findByApplicantIdInWithJobRole(@Param("applicantIds") List<String> applicantIds);

}