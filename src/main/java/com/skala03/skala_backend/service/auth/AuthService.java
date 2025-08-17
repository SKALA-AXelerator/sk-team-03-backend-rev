package com.skala03.skala_backend.service.auth;

import com.skala03.skala_backend.dto.auth.AuthDto;
import com.skala03.skala_backend.entity.auth.User;
import com.skala03.skala_backend.entity.auth.RefreshToken;
import com.skala03.skala_backend.repository.auth.UserRepository;
import com.skala03.skala_backend.repository.auth.RefreshTokenRepository;
import com.skala03.skala_backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

//    @Transactional
//    public AuthDto.AuthResponse signup(AuthDto.SignupRequest request) {
//        // 사용자 ID 중복 확인
//        if (userRepository.existsById(request.getUserId())) {
//            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
//        }
//
//        // 이메일 중복 확인
//        if (userRepository.existsByUserEmail(request.getUserEmail())) {
//            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
//        }
//
//        // 역할 유효성 검증
//        User.Role role;
//        try {
//            role = User.Role.valueOf(request.getUserRole());
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("유효하지 않은 역할입니다. (admin, interviewer만 가능)");
//        }
//
//        // 사용자 생성
//        User user = User.builder()
//                .userId(request.getUserId())
//                .userEmail(request.getUserEmail())
//                .userPassword(passwordEncoder.encode(request.getUserPassword()))
//                .userName(request.getUserName())
//                .userRole(role)
//                .build();
//
//        User savedUser = userRepository.save(user);
//
//        // 토큰 생성 및 저장
//        String accessToken = jwtUtil.generateAccessToken(savedUser.getUserEmail(), savedUser.getUserRole().name());
//        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUserEmail());
//        saveRefreshToken(savedUser.getUserId(), refreshToken);
//
//        log.info("회원가입 완료: {}", savedUser.getUserEmail());
//
//        return createAuthResponse(savedUser, accessToken, refreshToken);
//    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByUserEmail(request.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(user.getUserId());

        // 토큰 생성 및 저장
        String accessToken = jwtUtil.generateAccessToken(user.getUserEmail(), user.getUserRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserEmail());
        saveRefreshToken(user.getUserId(), refreshToken);

        log.info("로그인 완료: {}", user.getUserEmail());

        return createAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        // 리프레시 토큰 유효성 검증
        if (!jwtUtil.validateToken(refreshTokenValue)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 리프레시 토큰으로 토큰 정보 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        // 토큰 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다.");
        }

        // 사용자 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.delete(refreshToken);

        // 새로운 토큰 생성 및 저장
        String newAccessToken = jwtUtil.generateAccessToken(user.getUserEmail(), user.getUserRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserEmail());
        saveRefreshToken(user.getUserId(), newRefreshToken);

        log.info("토큰 재발급 완료: {}", user.getUserEmail());

        return createAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(user.getUserId());

        log.info("로그아웃 완료: {}", userEmail);
    }

    private void saveRefreshToken(String userId, String tokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private AuthDto.AuthResponse createAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000) // 초 단위로 변환
                .userInfo(AuthDto.AuthResponse.UserInfo.builder()
                        .userId(user.getUserId())
                        .userEmail(user.getUserEmail())
                        .userName(user.getUserName())
                        .userRole(user.getUserRole().name())
                        .build())
                .build();
    }
}