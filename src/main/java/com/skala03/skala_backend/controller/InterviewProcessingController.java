package com.skala03.skala_backend.controller;



import com.skala03.skala_backend.dto.InterviewProcessingDto;
import com.skala03.skala_backend.service.InterviewProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            description = "STT 데이터를 받아 AI 분석 후 평가 결과를 DB에 저장합니다. 직무명으로 DB에서 평가기준을 자동 조회합니다.")
    public ResponseEntity<InterviewProcessingDto.ProcessingResponse> processFullPipeline(
            @RequestBody InterviewProcessingDto.ProcessingRequest request) {

        try {
            log.info("📋 면접 처리 요청: sessionId={}, jobRoleName={}, 지원자수={}",
                    request.getSessionId(), request.getJobRoleName(),
                    request.getApplicantIds() != null ? request.getApplicantIds().size() : 0);

            // 입력 검증
            if (request.getSessionId() == null) {
                return ResponseEntity.badRequest().body(
                        InterviewProcessingDto.ProcessingResponse.builder()
                                .success(false)
                                .message("세션 ID는 필수입니다.")
                                .build()
                );
            }

            if (request.getJobRoleName() == null || request.getJobRoleName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        InterviewProcessingDto.ProcessingResponse.builder()
                                .success(false)
                                .message("직무명은 필수입니다.")
                                .build()
                );
            }

            if (request.getApplicantIds() == null || request.getApplicantIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        InterviewProcessingDto.ProcessingResponse.builder()
                                .success(false)
                                .message("지원자 ID 목록은 필수입니다.")
                                .build()
                );
            }

            if (request.getApplicantNames() == null || request.getApplicantNames().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        InterviewProcessingDto.ProcessingResponse.builder()
                                .success(false)
                                .message("지원자 이름 목록은 필수입니다.")
                                .build()
                );
            }

            if (request.getRawStt() == null) {
                return ResponseEntity.badRequest().body(
                        InterviewProcessingDto.ProcessingResponse.builder()
                                .success(false)
                                .message("STT 데이터는 필수입니다.")
                                .build()
                );
            }

            // 서비스 호출
            InterviewProcessingDto.ProcessingResponse response =
                    interviewProcessingService.processFullPipeline(request);

            if (response.isSuccess()) {
                log.info("✅ 면접 처리 완료: sessionId={}, 성공/실패={}/{}",
                        response.getSessionId(), response.getSuccessfulCount(), response.getFailedCount());
                return ResponseEntity.ok(response);
            } else {
                log.error("❌ 면접 처리 실패: sessionId={}, message={}",
                        response.getSessionId(), response.getMessage());
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    InterviewProcessingDto.ProcessingResponse.builder()
                            .success(false)
                            .message("잘못된 요청: " + e.getMessage())
                            .build()
            );

        } catch (Exception e) {
            log.error("면접 처리 중 서버 오류: ", e);
            return ResponseEntity.internalServerError().body(
                    InterviewProcessingDto.ProcessingResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }


}