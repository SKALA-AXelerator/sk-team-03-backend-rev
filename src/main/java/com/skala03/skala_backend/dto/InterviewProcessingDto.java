package com.skala03.skala_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class InterviewProcessingDto {

    // 프론트엔드로부터 받는 요청 (evaluation_criteria 제거, job_role_name으로 DB 조회)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingRequest {
        @JsonProperty("session_id")
        private Integer sessionId;

        @JsonProperty("applicant_ids")
        private List<String> applicantIds;

        @JsonProperty("applicant_names")
        private List<String> applicantNames;

        @JsonProperty("job_role_name")  // 이것으로 DB에서 평가기준 조회
        private String jobRoleName;

        @JsonProperty("raw_stt")
        private Object rawStt;  // JSON 또는 String
    }

    // FastAPI로 보내는 요청 (DB에서 조회한 평가기준 포함)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiRequest {
        @JsonProperty("session_id")
        private Integer sessionId;

        @JsonProperty("applicant_ids")
        private List<String> applicantIds;

        @JsonProperty("applicant_names")
        private List<String> applicantNames;

        @JsonProperty("job_role_name")
        private String jobRoleName;

        @JsonProperty("evaluation_criteria")  // DB에서 조회해서 구성
        private Map<String, Map<String, String>> evaluationCriteria;

        @JsonProperty("raw_stt")
        private Object rawStt;
    }

    // 응답 DTO는 동일
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingResponse {
        private boolean success;
        private String message;
        private Integer sessionId;
        private int totalProcessed;
        private int successfulCount;
        private int failedCount;
        private double totalProcessingTime;
    }
}