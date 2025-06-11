package com.skala03.skala_backend.controller;


import com.skala03.skala_backend.global.common.ApiResponse;
import com.skala03.skala_backend.dto.TranscriptionResult;
import com.skala03.skala_backend.service.TranscriptionService;
import com.skala03.skala_backend.service.VitoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
@RequestMapping("/api/interviewers/transcription")
@Tag(name = "면접관 API")
@RequiredArgsConstructor

public class TranscriptionController {

    private final VitoAuthService vitoAuthService;
    private final TranscriptionService transcriptionService;

    @PostMapping("/upload-audio")
    @Operation(summary = "음성 파일 업로드", description = "stt+화자 분리로 텍스트를 받아옵니다.")
    public Mono<ResponseEntity<ApiResponse<TranscriptionResult>>> uploadAudio(
            @RequestParam("file") MultipartFile file) throws IOException {

        // 1. 임시 디렉토리에 파일 저장
        Path tempDir = Files.createTempDirectory("");
        Path filePath = Paths.get(tempDir.toString(), file.getOriginalFilename());
        file.transferTo(filePath);
        File audioFile = filePath.toFile();
        audioFile.deleteOnExit();

        // 2. 인증 토큰 획득 → 전사 처리 → 결과 응답
        return vitoAuthService.getAccessToken()
                .flatMap(jwtToken -> transcriptionService.transcribeAndPollResult(audioFile, jwtToken))
                .timeout(Duration.ofMinutes(3))
                .map(result -> ResponseEntity.ok(ApiResponse.success("전사 성공", result)))
                .switchIfEmpty(Mono.just(ResponseEntity.noContent().build()));
    }
}