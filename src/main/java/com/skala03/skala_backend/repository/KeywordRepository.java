// Repository
package com.skala03.skala_backend.repository;

import com.skala03.skala_backend.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Integer> {
    /**
     * 키워드 이름으로 키워드 조회
     * @param keywordName 키워드 이름
     * @return 키워드 엔티티
     */
    Optional<Keyword> findByKeywordName(@Param("keywordName") String keywordName);

}
