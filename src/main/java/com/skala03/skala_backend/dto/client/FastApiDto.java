package com.skala03.skala_backend.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class FastApiDto {

    // 키워드 생성 요청
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordRequest {
        @JsonProperty("keyword_name")
        private String keywordName;

        @JsonProperty("keyword_detail")
        private String keywordDetail;
    }

    // 키워드 생성 응답
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordResponse {
        private boolean success;
        private String message;
        private Map<Integer, String> criteria;

        @JsonProperty("keyword_name")
        private String keywordName;

        @JsonProperty("error_detail")
        private String errorDetail;
    }

    // 파이프라인 응답
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineResponse {
        private boolean success;
        private String message;

        @JsonProperty("session_id")
        private Integer sessionId;

        @JsonProperty("raw_stt_s3_path")
        private String rawSttS3Path;

        @JsonProperty("job_role_name")
        private String jobRoleName;

        @JsonProperty("evaluation_results")
        private List<ApplicantResult> evaluationResults;

        @JsonProperty("total_processed")
        private Integer totalProcessed;

        @JsonProperty("successful_count")
        private Integer successfulCount;

        @JsonProperty("failed_count")
        private Integer failedCount;

        @JsonProperty("total_processing_time")
        private Double totalProcessingTime;

        @JsonProperty("step_times")
        private Map<String, Double> stepTimes;
    }

    // 지원자 결과
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantResult {
        @JsonProperty("applicant_id")
        private String applicantId;

        @JsonProperty("applicant_name")
        private String applicantName;

        @JsonProperty("qna_s3_path")
        private String qnaS3Path;

        @JsonProperty("pdf_s3_path")
        private String pdfS3Path;

        @JsonProperty("evaluation_json")
        private Map<String, Object> evaluationJson;
    }
}