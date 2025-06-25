package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.InterviewProcessingDto;
import com.skala03.skala_backend.service.InterviewProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviewers")
@Tag(name = "면접 처리 API", description = "면접 STT 처리 및 AI 평가")
@RequiredArgsConstructor
@Slf4j
public class InterviewProcessingController {

    private final InterviewProcessingService interviewProcessingService;

    @PostMapping("/process-full-pipeline")
    @Operation(summary = "면접 전체 파이프라인 처리",
            description = "STT 데이터를 받아 AI 분석을 완료한 후 최종 결과를 반환합니다.")
    public ResponseEntity<InterviewProcessingDto.ProcessingResponse> processFullPipeline(
            @RequestBody InterviewProcessingDto.ProcessingRequest request) {

        try {
            log.info("📋 면접 처리 요청: sessionId={}, jobRoleName={}, 지원자수={}",
                    request.getSessionId(), request.getJobRoleName(),
                    request.getApplicantIds() != null ? request.getApplicantIds().size() : 0);

            // 입력 검증
            validateRequest(request);

            // 🔄 동기 처리 - 완료까지 기다림 (기존 FastApiClient 사용)
            InterviewProcessingDto.ProcessingResponse result =
                    interviewProcessingService.processFullPipeline(request);

            if (result.isSuccess()) {
                log.info("✅ 면접 처리 완료: sessionId={}, 성공={}, 실패={}, 총 시간={}초",
                        request.getSessionId(), result.getSuccessfulCount(),
                        result.getFailedCount(), result.getTotalProcessingTime());

                return ResponseEntity.ok(result);
            } else {
                log.error("❌ 면접 처리 실패: sessionId={}, message={}",
                        request.getSessionId(), result.getMessage());

                return ResponseEntity.status(500).body(result);
            }

        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 요청: error={}", e.getMessage());

            InterviewProcessingDto.ProcessingResponse errorResponse =
                    InterviewProcessingDto.ProcessingResponse.builder()
                            .success(false)
                            .message("요청 검증 실패: " + e.getMessage())
                            .sessionId(request.getSessionId())
                            .totalProcessed(0)
                            .successfulCount(0)
                            .failedCount(0)
                            .totalProcessingTime(0.0)
                            .build();

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("❌ 면접 처리 중 서버 오류: error={}", e.getMessage(), e);

            InterviewProcessingDto.ProcessingResponse errorResponse =
                    InterviewProcessingDto.ProcessingResponse.builder()
                            .success(false)
                            .message("서버 내부 오류: " + e.getMessage())
                            .sessionId(request.getSessionId())
                            .totalProcessed(0)
                            .successfulCount(0)
                            .failedCount(0)
                            .totalProcessingTime(0.0)
                            .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 요청 데이터 검증
     */
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

        log.debug("✅ 요청 검증 완료: sessionId={}, 지원자수={}",
                request.getSessionId(), request.getApplicantIds().size());
    }
}