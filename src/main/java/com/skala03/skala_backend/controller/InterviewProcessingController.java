package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.InterviewProcessingDto;
import com.skala03.skala_backend.service.InterviewProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@RestController
@RequestMapping("/api/interviewers")
@Tag(name = "면접 처리 API", description = "면접 STT 처리 및 AI 평가")
@RequiredArgsConstructor
@Slf4j
public class InterviewProcessingController {

    private final InterviewProcessingService interviewProcessingService;

    @PostMapping("/process-full-pipeline")
    @Operation(summary = "면접 전체 파이프라인 처리",
            description = "STT 데이터를 받아 백그라운드에서 AI 분석을 시작합니다. 즉시 작업 ID를 반환하여 504 타임아웃을 방지합니다.")
    public ResponseEntity<Map<String, Object>> processFullPipeline(
            @RequestBody InterviewProcessingDto.ProcessingRequest request) {

        try {
            log.info(" 면접 처리 요청: sessionId={}, jobRoleName={}, 지원자수={}",
                    request.getSessionId(), request.getJobRoleName(),
                    request.getApplicantIds() != null ? request.getApplicantIds().size() : 0);

            // 입력 검증
            validateRequest(request);

            // 고유한 작업 ID 생성
            String jobId = UUID.randomUUID().toString();

            //  비동기로 처리 시작 (즉시 반환)
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("🔄 백그라운드 처리 시작: jobId={}, sessionId={}", jobId, request.getSessionId());

                    // 실제 처리 수행
                    InterviewProcessingDto.ProcessingResponse result =
                            interviewProcessingService.processFullPipeline(request);

                    if (result.isSuccess()) {
                        log.info("✅ 백그라운드 처리 완료: jobId={}, sessionId={}, 성공={}, 실패={}",
                                jobId, request.getSessionId(), result.getSuccessfulCount(), result.getFailedCount());
                    } else {
                        log.error("❌ 백그라운드 처리 실패: jobId={}, sessionId={}, message={}",
                                jobId, request.getSessionId(), result.getMessage());
                    }

                } catch (Exception e) {
                    log.error("❌ 백그라운드 처리 중 예외 발생: jobId={}, sessionId={}, error={}",
                            jobId, request.getSessionId(), e.getMessage(), e);
                }
            });

            // 즉시 작업 시작 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "jobId", jobId,
                    "sessionId", request.getSessionId(),
                    "status", "PROCESSING_STARTED",
                    "message", "면접 처리가 백그라운드에서 시작되었습니다.",
                    "totalApplicants", request.getApplicantIds().size(),
                    "estimatedTime", "약 3-5분 소요 예상",
                    "note", "처리가 완료되면 해당 세션의 지원자 상태가 업데이트됩니다."
            ));

        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 요청: sessionId={}, error={}", request.getSessionId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", e.getMessage(),
                    "sessionId", request.getSessionId() != null ? request.getSessionId() : "unknown"
            ));

        } catch (Exception e) {
            log.error("❌ 면접 처리 시작 중 서버 오류: sessionId={}, error={}",
                    request.getSessionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", "서버 내부 오류가 발생했습니다: " + e.getMessage(),
                    "sessionId", request.getSessionId() != null ? request.getSessionId() : "unknown"
            ));
        }
    }

    private void validateRequest(InterviewProcessingDto.ProcessingRequest request) {
        if (request.getSessionId() == null) {
            throw new IllegalArgumentException("세션 ID는 필수입니다.");
        }

        if (request.getJobRoleName() == null || request.getJobRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("직무명은 필수입니다.");
        }

        if (request.getApplicantIds() == null || request.getApplicantIds().isEmpty()) {
            throw new IllegalArgumentException("지원자 ID 목록은 필수입니다.");
        }

        if (request.getApplicantNames() == null || request.getApplicantNames().isEmpty()) {
            throw new IllegalArgumentException("지원자 이름 목록은 필수입니다.");
        }

        if (request.getApplicantIds().size() != request.getApplicantNames().size()) {
            throw new IllegalArgumentException("지원자 ID와 이름 목록의 개수가 일치하지 않습니다.");
        }

        if (request.getRawStt() == null) {
            throw new IllegalArgumentException("STT 데이터는 필수입니다.");
        }
    }
}