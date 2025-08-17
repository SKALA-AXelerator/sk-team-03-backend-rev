package com.skala03.skala_backend.dto.applicant;

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
        private String interviewStatus;  // 면접 상태
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
        private JobRoleInfo jobRole;  // 직무 정보 추가
        private List<QuestionInfo> questionList;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobRoleInfo {
        private String jobRoleName;
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
        private List<String> applicantIds;  // BasicInfo 대신 ID 리스트만
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
        private String name;  // 서버에서 ID로 조회하여 반환
        private String jobRoleName;
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

    @Data
    @NoArgsConstructor
    public static class StatusChangeRequest {
        private String interviewStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChangeResponse {
        private String applicantId;
        private String currentStatus;
        private String message;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionReorganizeRequest {
        private List<String> selectedApplicantIds; // 새 세션에 포함할 지원자들
        private String roomId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionReorganizeResponse {
        private Integer newSessionId;
        private List<String> newSessionApplicants;
        private List<SessionUpdateInfo> updatedSessions;
        private String message;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionUpdateInfo {
        private Integer sessionId;
        private List<String> remainingApplicants;
        private String action; // "updated" or "deleted" - 세션 상태 구분
    }
}