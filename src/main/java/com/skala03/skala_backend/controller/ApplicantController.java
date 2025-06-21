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
    @Operation(summary = "지원자별 질문 리스트와 직무 조회", description = "지원자별 질문 리스트와 직무 정보를 조회합니다.")
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
    @Operation(summary = "지원자 평가 정보 조회 (AI 필요)", description = "지원자 평가 정보를 조회합니다")
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

//    // 지원자 수동 추가 후 DB 처리 메서드
//    @PutMapping("/direct-add")
//    @Operation(summary = "지원자를 직접 골라서 면접에 추가", description = "특정 지원자를 선택해서 면접에 추가합니다.")
//    public ResponseEntity<?> directAddApplicant(
//            @RequestBody ApplicantDto.DetailedStatusUpdateRequest request) {
//        try {
//            applicantService.directAddApplicant(request);
//            return ResponseEntity.ok().build();
//        } catch (RuntimeException e) {
//            // 잘못된 상태값인 경우 400 Bad Request 반환
//            if (e.getMessage().contains("Invalid interview status")) {
//                return ResponseEntity.badRequest().body(e.getMessage());
//            }
//            return ResponseEntity.internalServerError().build();
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }

    // 개별 지원자 상태 변경
    @PutMapping("/{applicantId}/status")
    @Operation(summary = "개별 지원자 면접 상태 수정", description = "개별 지원자의 면접 상태를 수정합니다.")
    public ResponseEntity<?> updateApplicantStatus(
            @PathVariable String applicantId,
            @RequestBody ApplicantDto.StatusChangeRequest request) {
        try {
            ApplicantDto.StatusChangeResponse response = applicantService.updateApplicantStatus(applicantId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 지원자 없음 에러
            if (e.getMessage().contains("Applicant not found")) {
                return ResponseEntity.status(404).body("Applicant not found");
            }

            // 잘못된 상태값 에러
            if (e.getMessage().contains("Invalid interview status")) {
                return ResponseEntity.status(400).body("Invalid interview status");
            }

            // 기타 에러
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // ===== 추가 편의 API들 =====

    // 세션별 지원자 조회
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "특정 세션의 지원자 조회", description = "특정 세션의 지원자 리스트를 조회합니다.")
    public ResponseEntity<List<ApplicantDto.ApplicantInfo>> getApplicantsBySession(
            @PathVariable Integer sessionId) {
        try {
            List<ApplicantDto.ApplicantInfo> response = applicantService.getApplicantsBySession(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 직무별 지원자 조회
    @GetMapping("/job-role/{jobRoleId}")
    @Operation(summary = "특정 직무에 지원한 지원자 조회", description = "특정 직무에 지원한 지원자 리스트를 조회합니다.")
    public ResponseEntity<List<ApplicantDto.ApplicantInfo>> getApplicantsByJobRole(
            @PathVariable String jobRoleId) {
        try {
            List<ApplicantDto.ApplicantInfo> response = applicantService.getApplicantsByJobRole(jobRoleId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 세션 재편성 (지원자 수동 선택해서 새 세션 생성)
    @PostMapping("/sessions/reorganize")
    @Operation(summary = "세션 재편성", description = "선택된 지원자들로 새 세션을 만들고 기존 세션들을 재구성합니다.")
    public ResponseEntity<ApplicantDto.SessionReorganizeResponse> reorganizeSessions(
            @RequestBody ApplicantDto.SessionReorganizeRequest request) {
        try {
            ApplicantDto.SessionReorganizeResponse response = applicantService.reorganizeSessions(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}