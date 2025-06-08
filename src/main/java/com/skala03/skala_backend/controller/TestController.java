package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "테스트 API", description = "JWT 인증 테스트용 API")
public class TestController {

    @GetMapping("/public")
    @Operation(summary = "공개 API", description = "인증 없이 접근 가능한 API")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("공개 API 호출 성공", "인증 없이 접근 가능합니다."));
    }

    @GetMapping("/private")
    @Operation(summary = "인증 필요 API", description = "JWT 토큰이 필요한 API")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> privateEndpoint(
            @AuthenticationPrincipal UserDetails userDetails) {

        String message = String.format("인증된 사용자: %s", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("인증 API 호출 성공", message));
    }
}