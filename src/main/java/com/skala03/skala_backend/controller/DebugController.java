package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.entity.User;
import com.skala03.skala_backend.repository.UserRepository;
import com.skala03.skala_backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "백엔드 디버깅 API", description = "프론트 측에서는 쓰지 않습니다.")
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    @Operation(summary = "모든 사용자 조회", description = "DB에 있는 모든 사용자를 조회합니다.")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("사용자 목록 조회 성공", users));
    }

    @PostMapping("/create-test-user")
    @Operation(summary = "테스트 사용자 생성", description = "테스트용 사용자를 생성합니다.")
    public ResponseEntity<ApiResponse<String>> createTestUser() {
        try {
            // 기존 사용자 삭제
            userRepository.deleteById("test001");

            // 새 테스트 사용자 생성
            User testUser = User.builder()
                    .userId("test001")
                    .userEmail("test@skala.com")
                    .userPassword(passwordEncoder.encode("test123"))
                    .userName("테스트 사용자")
                    .userRole(User.Role.ADMIN)
                    .build();

            userRepository.save(testUser);

            log.info("테스트 사용자 생성 완료: test@skala.com / test123");
            return ResponseEntity.ok(ApiResponse.success("테스트 사용자 생성 완료",
                    "이메일: test@skala.com, 비밀번호: test123"));
        } catch (Exception e) {
            log.error("테스트 사용자 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 사용자 생성 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/generate-hash")
    @Operation(summary = "비밀번호 해시 생성", description = "입력한 비밀번호의 BCrypt 해시를 생성합니다.")
    public ResponseEntity<ApiResponse<String>> generatePasswordHash(@RequestParam String password) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            String result = String.format("원본: %s\n해시: %s", password, hashedPassword);

            log.info("비밀번호 해시 생성: {} -> {}", password, hashedPassword);
            return ResponseEntity.ok(ApiResponse.success("해시 생성 완료", result));

        } catch (Exception e) {
            log.error("해시 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("해시 생성 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-password")
    @Operation(summary = "비밀번호 검증", description = "이메일과 비밀번호가 일치하는지 확인합니다.")
    public ResponseEntity<ApiResponse<String>> verifyPassword(
            @RequestParam String email,
            @RequestParam String password) {

        try {
            User user = userRepository.findByUserEmail(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.ok(ApiResponse.success("검증 결과",
                        "사용자를 찾을 수 없습니다: " + email));
            }

            boolean matches = passwordEncoder.matches(password, user.getUserPassword());
            String result = String.format(
                    "이메일: %s\n입력 비밀번호: %s\nDB 해시: %s\n일치 여부: %s",
                    email, password, user.getUserPassword().substring(0, 20) + "...", matches
            );

            log.info("비밀번호 검증: {} - {}", email, matches);
            return ResponseEntity.ok(ApiResponse.success("검증 결과", result));

        } catch (Exception e) {
            log.error("비밀번호 검증 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("검증 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/update-user-password")
    @Operation(summary = "사용자 비밀번호 업데이트", description = "특정 사용자의 비밀번호를 새로 해시하여 업데이트합니다.")
    public ResponseEntity<ApiResponse<String>> updateUserPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {

        try {
            User user = userRepository.findByUserEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

            // 새 비밀번호로 해시 생성
            String hashedPassword = passwordEncoder.encode(newPassword);

            // 새 User 객체 생성 (기존 방식이 불변 객체라면)
            User updatedUser = User.builder()
                    .userId(user.getUserId())
                    .userEmail(user.getUserEmail())
                    .userPassword(hashedPassword)
                    .userName(user.getUserName())
                    .userRole(user.getUserRole())
                    .build();

            userRepository.save(updatedUser);

            String result = String.format("사용자: %s\n새 비밀번호: %s\n새 해시: %s",
                    email, newPassword, hashedPassword);

            log.info("사용자 비밀번호 업데이트 완료: {}", email);
            return ResponseEntity.ok(ApiResponse.success("비밀번호 업데이트 완료", result));

        } catch (Exception e) {
            log.error("비밀번호 업데이트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("업데이트 실패: " + e.getMessage()));
        }
    }
}