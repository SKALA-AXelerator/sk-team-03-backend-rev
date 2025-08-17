package com.skala03.skala_backend.repository.applicant;

import com.skala03.skala_backend.entity.applicant.Applicant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, String> {

    @Query("SELECT a FROM Applicant a WHERE a.applicantId IN :applicantIds")
    List<Applicant> findByApplicantIdIn(@Param("applicantIds") List<String> applicantIds);

    @Query("SELECT a FROM Applicant a WHERE a.sessionId = :sessionId")
    List<Applicant> findBySessionId(@Param("sessionId") Integer sessionId);

    @Query("SELECT a FROM Applicant a WHERE a.jobRole.jobRoleId = :jobRoleId")
    List<Applicant> findByJobRoleId(@Param("jobRoleId") String jobRoleId);

    // ===== 지원자별 질문 조회 메서드 추가 =====
    @Query(value = "SELECT q.question_text FROM questions q " +
            "JOIN applicant_questions aq ON q.question_id = aq.question_id " +
            "WHERE aq.applicant_id = :applicantId " +
            "ORDER BY q.question_id", nativeQuery = true)
    List<String> findQuestionsByApplicantId(@Param("applicantId") String applicantId);

    // ✅ 올바른 방법: sessions 테이블에서 최대 session_id 조회
    @Query(value = "SELECT COALESCE(MAX(session_id), 0) FROM sessions", nativeQuery = true)
    Integer findMaxSessionIdFromSessions();

    // 세션의 지원자 목록 업데이트
    @Modifying
    @Query(value = "UPDATE sessions SET applicants_user_id = :applicantIds WHERE session_id = :sessionId", nativeQuery = true)
    void updateSessionApplicants(@Param("sessionId") Integer sessionId, @Param("applicantIds") String applicantIds);

    // ✅ 기존 세션 정보를 완전히 복사하여 새 세션 생성 (session_id와 applicants_user_id만 다름)
    @Modifying
    @Query(value = "INSERT INTO sessions (session_id, room_id, session_name, session_date, session_location, session_time, session_status, interviewers_user_id, applicants_user_id, raw_data_path) " +
            "SELECT ?1, room_id, session_name, session_date, session_location, session_time, session_status, interviewers_user_id, ?2, raw_data_path " +
            "FROM sessions WHERE session_id = ?3", nativeQuery = true)
    void createNewSessionFromExisting(Integer newSessionId, String newApplicantIds, Integer originalSessionId);

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
    @Query("SELECT a FROM Applicant a JOIN FETCH a.jobRole WHERE a.applicantId IN :applicantIds")
    List<Applicant> findByApplicantIdInWithJobRole(@Param("applicantIds") List<String> applicantIds);

    // JobRole과 함께 조회
    @Query("SELECT a FROM Applicant a JOIN FETCH a.jobRole WHERE a.applicantId = :applicantId")
    Optional<Applicant> findByIdWithJobRole(@Param("applicantId") String applicantId);

    // 세션 삭제 (빈 세션 정리용)
    @Modifying
    @Query(value = "DELETE FROM sessions WHERE session_id = :sessionId", nativeQuery = true)
    void deleteSessionById(@Param("sessionId") Integer sessionId);

    // 세션 존재 여부 확인
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM sessions WHERE session_id = :sessionId", nativeQuery = true)
    boolean existsSessionById(@Param("sessionId") Integer sessionId);

    // ZIP 다운로드용 메서드 추가
    @Query("SELECT a FROM Applicant a WHERE a.jobRole.jobRoleId = :jobRoleId AND a.individualPdfPath IS NOT NULL")
    List<Applicant> findByJobRoleIdWithPdfPath(@Param("jobRoleId") String jobRoleId);

    @Query("SELECT a FROM Applicant a WHERE a.jobRole.jobRoleId = :jobRoleId AND a.individualQnaPath IS NOT NULL")
    List<Applicant> findByJobRoleIdWithQnaPath(@Param("jobRoleId") String jobRoleId);


}