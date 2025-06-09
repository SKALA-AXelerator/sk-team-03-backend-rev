package com.skala03.skala_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.skala03.skala_backend.dto.ApplicantDto;
import com.skala03.skala_backend.service.ApplicantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applicants")
@Tag(name = "지원자 API", description = "지원자 조회, 질문 리스트 조회, 상태 변경 등 지원자 관련 API")
public class ApplicantController {

    @Autowired
    private ApplicantService applicantService;

    // 전체 지원자 리스트 조회
    @GetMapping
    @Operation(summary = "모든 지원자 조회", description = "전체 지원자 리스트를 조회합니다.")
    public ResponseEntity<ApplicantDto.ListResponse> getAllApplicants() {
        try {
            ApplicantDto.ListResponse response = applicantService.getAllApplicants();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 지원자별 질문 리스트 조회
    @PostMapping("/questions")
    @Operation(summary = "지원자별 질문 리스트 조회", description = "지원자별 질문 리스트를 조회합니다.")
    public ResponseEntity<ApplicantDto.QuestionsResponse> getApplicantQuestions(
            @RequestBody ApplicantDto.QuestionsRequest request) {
        try {
            ApplicantDto.QuestionsResponse response = applicantService.getApplicantQuestions(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 지원자 평가 (AI 분석)
    @PostMapping("/evaluations")
    @Operation(summary = "지원자 평가 정보 조회", description = "지원자 평가 정보를 조회합니다")
    public ResponseEntity<List<ApplicantDto.EvaluationResponse>> evaluateApplicants(
            @RequestBody ApplicantDto.EvaluationRequest request) {
        try {
            List<ApplicantDto.EvaluationResponse> response = applicantService.evaluateApplicants(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 면접 완료 상태로 일괄 변경
    @PutMapping("/status/interview-complete")
    @Operation(summary = "면접 완료 상태로 일괄 변경", description = "지원자들의 면접 상태를 '완료'로 업데이트합니다.")
    public ResponseEntity<Void> updateToInterviewComplete(
            @RequestBody ApplicantDto.StatusUpdateRequest request) {
        try {
            applicantService.updateToInterviewComplete(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 지원자 상태 업데이트 (면접관, 방 정보 + 면접 상태 포함)
    @PutMapping("/update-status")
    @Operation(summary = "지원자 상태 업데이트", description = "지원자의 면접 상태 정보를 업데이트합니다.")
    public ResponseEntity<?> updateApplicantStatus(
            @RequestBody ApplicantDto.DetailedStatusUpdateRequest request) {
        try {
            applicantService.updateApplicantStatus(request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // 잘못된 상태값인 경우 400 Bad Request 반환
            if (e.getMessage().contains("Invalid interview status")) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

//    // ===== 추가 편의 API들 =====
//
//    // 세션별 지원자 조회
//    @GetMapping("/session/{sessionId}")
//    public ResponseEntity<List<ApplicantDto.ApplicantInfo>> getApplicantsBySession(
//            @PathVariable Integer sessionId) {
//        try {
//            List<ApplicantDto.ApplicantInfo> response = applicantService.getApplicantsBySession(sessionId);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    // 직무별 지원자 조회
//    @GetMapping("/job-role/{jobRoleId}")
//    public ResponseEntity<List<ApplicantDto.ApplicantInfo>> getApplicantsByJobRole(
//            @PathVariable String jobRoleId) {
//        try {
//            List<ApplicantDto.ApplicantInfo> response = applicantService.getApplicantsByJobRole(jobRoleId);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }
}