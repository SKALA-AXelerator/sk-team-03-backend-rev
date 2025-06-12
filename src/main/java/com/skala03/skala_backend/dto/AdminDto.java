package com.skala03.skala_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AdminDto {

    // 1. 키워드 조회 응답 (/api/admin/keywords)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeywordResponse {
        private Integer keywordId;
        private String keywordName;
        private String keywordDetail;
        private List<KeywordCriteriaInfo> keywordCriteria;
    }

    // 키워드 평가 기준 정보 (공통 사용)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeywordCriteriaInfo {
        private Integer keywordScore;
        private String keywordGuideline;
    }

    // 2. 키워드 생성 요청 (/api/admin/create-keywords)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateKeywordRequest {
        private String keywordName;
        private String keywordDetail;
        private List<KeywordCriteriaInfo> keywordCriteria;
    }

    // 3. AI 생성 요청 (/api/admin/ai-generate-keywords/{keyword_id})
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiGenerateRequest {
        private String keywordName;
        private String keywordDetail;
    }

    // 4. 키워드 수정 요청 (/api/admin/change-keywords/{keyword_id})
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateKeywordRequest {
        private String keywordDetail;
        private List<KeywordCriteriaInfo> keywordCriteria;
    }

    // 5. AI 생성 응답 - 수정됨
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiGenerateResponse {
        private List<KeywordCriteriaInfo> keywordCriteria; // generatedCriteria → keywordCriteria로 변경
        private String message;
    }

    // 직군 및 키워드 매핑 조회 응답
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobRoleKeywordResponse {
        private String jobRoleId;
        private String jobRoleName;
        private List<KeywordWithCriteriaInfo> keywords;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeywordWithCriteriaInfo {
        private Integer keywordId;
        private String keywordTitle;    // keyword_name과 동일
        private String keywordDetail;
        private Boolean keywordSelected;
        private List<KeywordCriteriaInfo> keywordCriteria;
    }

    // 직군 조회 응답
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobRoleResponse {
        private String jobRoleId;
        private String jobRoleName;
    }

    // 키워드 선택 상태 수정 요청
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateKeywordSelectionRequest {
        private List<KeywordSelectionInfo> keywords;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeywordSelectionInfo {
        private Integer keywordId;
        private Boolean selected;
    }

    // 키워드 선택 상태 수정 응답
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateKeywordSelectionResponse {
        private String jobRoleId;
        private String message;
        private List<KeywordSelectionInfo> updatedKeywords;
    }

    // 새로운 키워드에 대한 평가 기준 생성 요청을 위한 DTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewKeywordAiGenerateRequest {
        private String keywordName;   // 신규 키워드 이름
        private String keywordDetail; // 신규 키워드 상세 설명 (선택)
    }
}