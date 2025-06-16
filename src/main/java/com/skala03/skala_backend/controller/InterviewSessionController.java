package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.InterviewSessionDto;
import com.skala03.skala_backend.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@Tag(name = "면접 세션 관리 API", description = "면접 세션 관련 API")
@RestController
@RequestMapping("/api/interviewers")
@CrossOrigin(origins = "*")
public class InterviewSessionController {

    @Autowired
    private InterviewSessionService interviewSessionService;

    @Operation(summary = "세션 리스트 입장", description = "사용자가 특정 방의 세션 리스트에 입장")
    @PostMapping("/enter-session-list/{roomId}/{userId}")
    public ResponseEntity<Void> enterSessionList(
            @PathVariable String roomId,
            @PathVariable String userId) {
        interviewSessionService.enterSessionList(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "세션 종료", description = "사용자가 세션을 종료")
    @PostMapping("/end-session/{roomId}/{userId}")
    public ResponseEntity<Void> endSession(
            @PathVariable String roomId,
            @PathVariable String userId) {
        interviewSessionService.endSession(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "참가자 상태 조회", description = "특정 방에서 특정 사용자의 참가 상태 조회")
    @GetMapping("/status/{roomId}/{userId}")
    public ResponseEntity<Map<String, Object>> getParticipantStatus(
            @PathVariable String roomId,
            @PathVariable String userId) {
        return ResponseEntity.ok(interviewSessionService.getParticipantStatus(roomId, userId));
    }

    @Operation(summary = "면접 시작", description = "방장이 면접을 시작")
    @PostMapping("/start")
    public ResponseEntity<Boolean> startInterview(
            @Valid @RequestBody InterviewSessionDto.StartInterviewRequest request) {
        boolean success = interviewSessionService.startInterview(
                request.getRoomId(),
                request.getSessionId(),
                request.getLeaderUserId()
        );
        return ResponseEntity.ok(success);
    }

    @Operation(summary = "세션 상태 조회", description = "특정 세션의 현재 상태 조회")
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.getSessionStatus(sessionId));
    }

    @Operation(summary = "세션 시작", description = "세션 상태를 IN_PROGRESS로 변경")
    @PutMapping("/status/{sessionId}/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.updateSessionToInProgress(sessionId));
    }

    @Operation(summary = "세션 완료", description = "세션 상태를 COMPLETED로 변경")
    @PutMapping("/status/{sessionId}/complete")
    public ResponseEntity<Map<String, Object>> completeSession(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.updateSessionToCompleted(sessionId));
    }

    @Operation(summary = "중간 리뷰 조회", description = "여러 지원자의 중간 리뷰 텍스트 조회")
    @PostMapping("/middle-reviews")
    public ResponseEntity<Map<String, String>> getMiddleReviewTexts(
            @Valid @RequestBody InterviewSessionDto.MiddleReviewsRequest request) {
        Map<String, String> middleReviews = interviewSessionService.getMiddleReviewTexts(request.getApplicantIds());
        return ResponseEntity.ok(middleReviews);
    }

    @Operation(summary = "최종 평가 조회", description = "여러 지원자의 최종 평가 정보 조회")
    @PostMapping("/final-reviews")
    public ResponseEntity<List<Map<String, Object>>> getFinalReviews(
            @Valid @RequestBody InterviewSessionDto.FinalReviewsRequest request) {
        List<Map<String, Object>> finalReviews = interviewSessionService.getFinalReviews(request.getApplicantIds());
        return ResponseEntity.ok(finalReviews);
    }
}