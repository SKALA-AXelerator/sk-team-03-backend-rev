package com.skala03.skala_backend.dto;

import com.skala03.skala_backend.entity.Session;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class InterviewSessionDto {

    @Data
    public static class StartInterviewRequest {
        @NotBlank(message = "방 ID는 필수입니다")
        private String roomId;

        @NotNull(message = "세션 ID는 필수입니다")
        private Integer sessionId;

        @NotBlank(message = "방장 사용자 ID는 필수입니다")
        private String leaderUserId;
    }

    @Data
    public static class EnterSessionRequest {
        @NotBlank(message = "방 ID는 필수입니다")
        private String roomId;

        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
    }

    @Data
    @AllArgsConstructor
    public static class SessionListResponse {
        private Integer sessionId;
        private String sessionName;
        private LocalDateTime sessionDate;
        private LocalDateTime sessionTime;
        private String sessionLocation;
        private Session.SessionStatus sessionStatus;
        private List<String> interviewers;
        private List<String> applicants;
        private boolean canStart;
    }

    /**
     * 여러 지원자 최종 평가 정보 조회 요청 DTO
     */
    @Data
    public static class FinalReviewsRequest {
        @NotNull(message = "지원자 ID 목록은 필수입니다.")
        @NotEmpty(message = "지원자 ID 목록이 비어있을 수 없습니다.")
        private List<String> applicantIds;
    }

    @Data
    public static class MiddleReviewsRequest {
        @NotNull(message = "지원자 ID 목록은 필수입니다.")
        @NotEmpty(message = "지원자 ID 목록이 비어있을 수 없습니다.")
        private List<String> applicantIds;
    }

}