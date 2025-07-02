// ========================================
// 3. SERVICE LAYER - 비즈니스 로직
// ========================================

// TranscriptionService.java - CompletableFuture 기반 서비스
package com.skala03.skala_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala03.skala_backend.dto.TranscriptionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final RestTemplate restTemplate;
    private final VitoAuthService vitoAuthService;
    private final ObjectMapper objectMapper;

    // 메모리에 상태 저장 (실제 운영환경에서는 Redis나 DB 사용 권장)
    private final Map<String, String> transcriptionStatusMap = new ConcurrentHashMap<>();
    private final Map<String, TranscriptionResult> transcriptionResultMap = new ConcurrentHashMap<>();
    private final Map<String, String> transcriptionErrorMap = new ConcurrentHashMap<>();

    public String startTranscription(MultipartFile file, Integer speakerCount) throws IOException {

        // 1. 고유 ID 생성
        String transcriptionId = UUID.randomUUID().toString();

        // 2. 파일 검증
        validateFile(file);

        // 3. 임시 파일 저장
        File audioFile = saveTemporaryFile(file);

        // 4. 상태를 processing으로 설정
        transcriptionStatusMap.put(transcriptionId, "processing");

        // 5. CompletableFuture로 비동기 작업 실행
        log.info(" 메인 스레드에서 작업 시작: {} [스레드: {}]",
                transcriptionId, Thread.currentThread().getName());

        CompletableFuture.runAsync(() -> {
                    processTranscription(transcriptionId, audioFile, speakerCount);
                }, getTranscriptionExecutor())
                .exceptionally(throwable -> {
                    log.error(" CompletableFuture 예외 발생: {}", transcriptionId, throwable);
                    transcriptionStatusMap.put(transcriptionId, "failed");
                    transcriptionErrorMap.put(transcriptionId, "비동기 작업 실패: " + throwable.getMessage());
                    return null;
                });

        log.info(" 메인 스레드 완료, 즉시 반환: {}", transcriptionId);
        return transcriptionId;
    }

    /**
     * 실제 전사 처리 로직 (CompletableFuture에서 실행)
     */
    private void processTranscription(String transcriptionId, File audioFile, Integer speakerCount) {
        try {
            log.info(" 비동기 전사 작업 시작: {} [스레드: {}]",
                    transcriptionId, Thread.currentThread().getName());

            // 1. 인증 토큰 획득
            log.info("Vito 인증 토큰 획득 시작: {}", transcriptionId);
            String jwtToken = vitoAuthService.getAccessToken();
            log.info("Vito 인증 토큰 획득 완료: {}", transcriptionId);

            // 2. Vito API에 전사 요청
            log.info("Vito API 전사 요청 시작: {}", transcriptionId);
            String vitoTranscriptionId = transcribe(audioFile, jwtToken, speakerCount);
            log.info("Vito API 전사 요청 완료: {} → vitoId: {}", transcriptionId, vitoTranscriptionId);

            // 3. 결과 폴링 (완료될 때까지 대기)
            log.info("Vito API 결과 폴링 시작: {}", transcriptionId);
            TranscriptionResult result = pollForResult(vitoTranscriptionId, jwtToken);
            log.info("Vito API 결과 폴링 완료: {}", transcriptionId);

            // 4. 결과 저장
            transcriptionStatusMap.put(transcriptionId, "completed");
            transcriptionResultMap.put(transcriptionId, result);

            log.info(" 전사 작업 완료: {} [스레드: {}]",
                    transcriptionId, Thread.currentThread().getName());

        } catch (Exception e) {
            log.error(" 전사 작업 실패: {} [스레드: {}] - 오류: {}",
                    transcriptionId, Thread.currentThread().getName(), e.getMessage(), e);
            transcriptionStatusMap.put(transcriptionId, "failed");
            transcriptionErrorMap.put(transcriptionId, e.getMessage());
        }
    }

    /**
     * CompletableFuture용 커스텀 스레드 풀
     */
    private Executor getTranscriptionExecutor() {
        return Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r);
            t.setName("CF-Transcription-" + t.getId());
            t.setDaemon(false); // 메인 스레드 종료시에도 작업 완료까지 대기
            return t;
        });
    }

    /**
     * 파일 검증 로직
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("업로드된 파일이 비어있습니다.");
        }

        if (file.getSize() > 500 * 1024 * 1024) { // 500MB 제한
            throw new IOException("파일 크기가 500MB를 초과합니다.");
        }
    }

    /**
     * 임시 파일 저장
     */
    private File saveTemporaryFile(MultipartFile file) throws IOException {
        Path tempDir = Files.createTempDirectory("transcription_");
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "audio_" + System.currentTimeMillis();
        }

        Path filePath = Paths.get(tempDir.toString(), originalFilename);
        file.transferTo(filePath);
        File audioFile = filePath.toFile();
        audioFile.deleteOnExit(); // JVM 종료시 삭제

        return audioFile;
    }

    private String transcribe(File audioFile, String jwtToken, Integer speakerCount) {
        try {
            log.info("Vito 전사 요청 준비 - 파일: {}, 화자수: {}",
                    audioFile.getName(), speakerCount);

            // 1. 파일 리소스 생성
            FileSystemResource fileResource = new FileSystemResource(audioFile);

            // 2. 설정값 처리
            int spkCount = (speakerCount != null) ? speakerCount : 6;

            // 3. JSON 설정 생성 (paragraph 제거됨)
            String configJson = String.format("""
            {   "model_name": "sommers",
                "use_diarization": true,
                "diarization": {
                    "spk_count": %d
                },
                "use_itn": false,
                "use_disfluency_filter": false,
                "use_profanity_filter": false
            }
            """, spkCount);

            log.debug("Vito 설정: {}", configJson);

            // 4. Multipart 폼 데이터 생성
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("file", fileResource);
            formData.add("config", configJson);

            // 5. HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(jwtToken);

            // 6. HTTP 요청 엔티티 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            // 7. API 호출
            String baseUrl = "https://openapi.vito.ai";
            log.info("Vito API 호출 시작 - URL: {}/v1/transcribe", baseUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/v1/transcribe",
                    requestEntity,
                    String.class
            );

            log.info("Vito API 응답 상태: {}", response.getStatusCode());
            log.debug("Vito API 응답 내용: {}", response.getBody());

            // 8. 응답에서 ID 추출
            String vitoId = extractIdFromResponse(response.getBody());
            log.info("Vito 전사 요청 성공 - vitoId: {}", vitoId);

            return vitoId;

        } catch (Exception e) {
            log.error("전사 요청 실패 - 파일: {}, 오류: {}", audioFile.getName(), e.getMessage(), e);
            throw new RuntimeException("전사 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 응답에서 ID 추출
     */
    private String extractIdFromResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            return (String) responseMap.get("id");
        } catch (Exception e) {
            // 백업 파싱 방법
            int idStart = response.indexOf(":\"") + 2;
            int idEnd = response.indexOf("\"", idStart);
            return response.substring(idStart, idEnd);
        }
    }

    /**
     * 결과 폴링 - 완료될 때까지 주기적으로 확인
     */
    private TranscriptionResult pollForResult(String vitoTranscriptionId, String jwtToken) {
        String baseUrl = "https://openapi.vito.ai";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        log.info("Vito 폴링 시작 - vitoId: {}", vitoTranscriptionId);

        //  대기 (10초 * 200회)
        for (int i = 0; i < 200; i++) {
            try {
                log.debug("Vito 폴링 시도 {}/300 - vitoId: {}", i + 1, vitoTranscriptionId);

                ResponseEntity<TranscriptionResult> response = restTemplate.exchange(
                        baseUrl + "/v1/transcribe/" + vitoTranscriptionId,
                        HttpMethod.GET,
                        requestEntity,
                        TranscriptionResult.class
                );

                TranscriptionResult result = response.getBody();
                log.debug("Vito 응답 상태: {} - vitoId: {}",
                        result != null ? result.getStatus() : "null", vitoTranscriptionId);

                // 완료되면 결과 반환
                if (result != null && "completed".equals(result.getStatus())) {
                    log.info("Vito 전사 완료 - vitoId: {}", vitoTranscriptionId);
                    return result;
                }

                // 10초 대기
                Thread.sleep(10000);

            } catch (Exception e) {
                log.error("Vito 폴링 중 오류 - 시도 {}/300, vitoId: {}, 오류: {}",
                        i + 1, vitoTranscriptionId, e.getMessage());
                if (i == 299) { // 마지막 시도
                    throw new RuntimeException("전사 결과 조회 실패: " + e.getMessage(), e);
                }
            }
        }

        throw new RuntimeException("전사 작업 시간 초과 - vitoId: " + vitoTranscriptionId);
    }

    // 상태 조회 메서드들
    public TranscriptionResult getTranscriptionResult(String transcriptionId) {
        return transcriptionResultMap.get(transcriptionId);
    }

    public String getTranscriptionStatus(String transcriptionId) {
        return transcriptionStatusMap.get(transcriptionId);
    }

    public String getTranscriptionError(String transcriptionId) {
        return transcriptionErrorMap.get(transcriptionId);
    }
}
