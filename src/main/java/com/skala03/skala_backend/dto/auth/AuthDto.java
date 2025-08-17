package com.skala03.skala_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "로그인 요청")
    public static class LoginRequest {

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Schema(description = "이메일", example = "admin@skala.com")
        private String userEmail;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Schema(description = "비밀번호", example = "password123")
        private String userPassword;
    }

//    @Getter
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Schema(description = "회원가입 요청")
//    public static class SignupRequest {
//
//        @NotBlank(message = "사용자 ID는 필수입니다")
//        @Schema(description = "사용자 ID", example = "admin001")
//        private String userId;
//
//        @NotBlank(message = "이메일은 필수입니다")
//        @Email(message = "올바른 이메일 형식이 아닙니다")
//        @Schema(description = "이메일", example = "admin@skala.com")
//        private String userEmail;
//
//        @NotBlank(message = "비밀번호는 필수입니다")
//        @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
//        @Schema(description = "비밀번호", example = "password123")
//        private String userPassword;
//
//        @NotBlank(message = "이름은 필수입니다")
//        @Schema(description = "담당자 이름", example = "관리자")
//        private String userName;
//
//        @NotBlank(message = "역할은 필수입니다")
//        @Schema(description = "사용자 역할", example = "admin", allowableValues = {"admin", "interviewer"})
//        private String userRole;
//    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "토큰 재발급 요청")
    public static class RefreshTokenRequest {

        @NotBlank(message = "리프레시 토큰은 필수입니다")
        @Schema(description = "리프레시 토큰")
        private String refreshToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 응답")
    public static class AuthResponse {

        @Schema(description = "액세스 토큰")
        private String accessToken;

        @Schema(description = "리프레시 토큰")
        private String refreshToken;

        @Schema(description = "토큰 타입", example = "Bearer")
        private String tokenType;

        @Schema(description = "액세스 토큰 만료 시간 (초)")
        private Long expiresIn;

        @Schema(description = "사용자 정보")
        private UserInfo userInfo;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "사용자 정보")
        public static class UserInfo {

            @Schema(description = "사용자 ID")
            private String userId;

            @Schema(description = "이메일")
            private String userEmail;

            @Schema(description = "이름")
            private String userName;

            @Schema(description = "역할")
            private String userRole;
        }
    }
}