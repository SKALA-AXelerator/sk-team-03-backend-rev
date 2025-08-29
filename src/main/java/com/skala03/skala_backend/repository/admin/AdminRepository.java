package com.skala03.skala_backend.repository.admin;

import com.skala03.skala_backend.entity.admin.Keyword;
import com.skala03.skala_backend.entity.admin.KeywordCriteria;
import com.skala03.skala_backend.entity.admin.JobRole;
import com.skala03.skala_backend.entity.admin.JobRoleKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AdminRepository extends JpaRepository<Keyword, Integer> {

    // === Keyword 관련 메서드들 ===

    // 키워드 이름으로 중복 체크
    @Query("SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END FROM Keyword k WHERE k.keywordName = :keywordName")
    boolean existsByKeywordName(@Param("keywordName") String keywordName);

    // === KeywordCriteria 관련 메서드들 ===

    // JPQL → nativeQuery
    @Query(value = "SELECT * FROM keyword_criteria WHERE keyword_id = :keywordId ORDER BY keyword_score DESC", nativeQuery = true)
    List<KeywordCriteria> findCriteriaByKeywordId(@Param("keywordId") Integer keywordId);

    // 특정 키워드의 모든 평가기준 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM KeywordCriteria kc WHERE kc.keywordId = :keywordId")
    void deleteCriteriaByKeywordId(@Param("keywordId") Integer keywordId);

    // 평가기준 개별 삽입
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO keyword_criteria (keyword_id, keyword_score, keyword_guideline) VALUES (:keywordId, :score, :guideline)", nativeQuery = true)
    void insertCriteria(@Param("keywordId") Integer keywordId, @Param("score") Integer score, @Param("guideline") String guideline);

    // 모든 직군 조회
    @Query(value = "SELECT * FROM job_roles", nativeQuery = true)
    List<JobRole> findAllJobRoles();

    // 모든 키워드 조회 (직군-키워드 매핑용)
    @Query(value = "SELECT * FROM keywords ORDER BY keyword_id", nativeQuery = true)
    List<Keyword> findAllKeywords();

    // 특정 직군의 키워드 매핑 정보 조회
    @Query(value = "SELECT * FROM job_role_keywords WHERE job_role_id = :jobRoleId", nativeQuery = true)
    List<JobRoleKeyword> findJobRoleKeywordsByJobRoleId(@Param("jobRoleId") String jobRoleId);

    // 모든 직군-키워드 매핑 정보 조회
    @Query(value = "SELECT * FROM job_role_keywords", nativeQuery = true)
    List<JobRoleKeyword> findAllJobRoleKeywords();

    // 특정 직군의 특정 키워드 매핑 정보 조회
    @Query(value = "SELECT * FROM job_role_keywords WHERE job_role_id = :jobRoleId AND keyword_id = :keywordId", nativeQuery = true)
    JobRoleKeyword findJobRoleKeyword(@Param("jobRoleId") String jobRoleId, @Param("keywordId") Integer keywordId);

    // 키워드 선택 상태 업데이트 (있으면 UPDATE, 없으면 INSERT)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO job_role_keywords (job_role_id, keyword_id, selected) VALUES (:jobRoleId, :keywordId, :selected) " +
            "ON DUPLICATE KEY UPDATE selected = :selected", nativeQuery = true)
    void upsertJobRoleKeyword(@Param("jobRoleId") String jobRoleId, @Param("keywordId") Integer keywordId, @Param("selected") Boolean selected);

    /**
     * 직무명으로 해당 직무에 연결된 모든 키워드의 평가 기준 조회
     * @param jobRoleName 직무명 (예: "AI/Data", "반도체", "제조", "금융")
     * @return Object[] 배열 리스트 - [keyword_id, keyword_name, keyword_score, keyword_guideline]
     */
    @Query(value = """
        SELECT 
            k.keyword_id,
            k.keyword_name, 
            kc.keyword_score,
            kc.keyword_guideline
        FROM job_roles jr
        JOIN job_role_keywords jrk ON jr.job_role_id = jrk.job_role_id
        JOIN keywords k ON jrk.keyword_id = k.keyword_id  
        JOIN keyword_criteria kc ON k.keyword_id = kc.keyword_id
        WHERE jr.job_role_name = :jobRoleName 
            AND jrk.selected = true
        ORDER BY k.keyword_id, kc.keyword_score DESC
        """, nativeQuery = true)
    List<Object[]> findEvaluationCriteriaByJobRoleName(@Param("jobRoleName") String jobRoleName);


}