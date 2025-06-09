package com.skala03.skala_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

public class ApplicantDto {

    // ===== 전체 지원자 리스트 조회 =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<ApplicantInfo> applicants;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantInfo {
        private String id;
        private String name;
    }

    // ===== 지원자별 질문 관련 =====
    @Data
    @NoArgsConstructor
    public static class QuestionsRequest {
        private List<String> applicantIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionsResponse {
        private List<QuestionInfo> questionList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionInfo {
        private String id;
        private String name;
        private List<String> questions;
    }

    // ===== 지원자 평가 관련 =====
    @Data
    @NoArgsConstructor
    public static class EvaluationRequest {
        private List<BasicInfo> applicants;
        private String interviewText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfo {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationResponse {
        private String id;
        private String name;
        private List<KeywordEvaluation> evaluations;
        private String summaryUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordEvaluation {
        private String keyword;
        private int score;
        private String content;
    }

    // ===== 상태 변경 관련 =====
    @Data
    @NoArgsConstructor
    public static class StatusUpdateRequest {
        private List<String> applicationIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedStatusUpdateRequest {
        private List<String> interviewerIds;
        private List<String> applicantIds;
        private String room;
        private String interviewStatus;  // 추가: 변경할 면접 상태
    }
}