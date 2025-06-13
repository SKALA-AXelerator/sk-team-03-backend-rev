package com.skala03.skala_backend.dto;



import com.skala03.skala_backend.entity.RoomParticipant;
import com.skala03.skala_backend.entity.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class InterviewSessionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantStatusResponse {
        private RoomParticipant.ParticipantStatus status;
        private LocalDateTime lastPingAt;
        private String message;
    }

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
        private boolean canStart; // 모든 면접관이 대기 중인지
    }

    @Data
    @AllArgsConstructor
    public static class InterviewStatusResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
