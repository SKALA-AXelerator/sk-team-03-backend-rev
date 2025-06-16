package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.InterviewSessionDto;
import com.skala03.skala_backend.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/interviewers")
@CrossOrigin(origins = "*")
public class InterviewSessionController {

    @Autowired
    private InterviewSessionService interviewSessionService;

    /**
     * 세션 리스트 입장 - 성공 시 200 OK
     */
    @PostMapping("/enter-session-list/{roomId}/{userId}")
    public ResponseEntity<Void> enterSessionList(
            @PathVariable String roomId,
            @PathVariable String userId) {
        interviewSessionService.enterSessionList(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 세션 종료 - 성공 시 200 OK
     */
    @PostMapping("/end-session/{roomId}/{userId}")
    public ResponseEntity<Void> endSession(
            @PathVariable String roomId,
            @PathVariable String userId) {
        interviewSessionService.endSession(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 참가자 상태 조회 - 상태값과 시간만 직접 반환
     */
    @GetMapping("/status/{roomId}/{userId}")
    public ResponseEntity<Map<String, Object>> getParticipantStatus(
            @PathVariable String roomId,
            @PathVariable String userId) {
        return ResponseEntity.ok(interviewSessionService.getParticipantStatus(roomId, userId));
    }

    /**
     * 면접 시작 - boolean 값만 반환
     */
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
    /**
     * 세션 상태 조회 - sessionStatus만 반환
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.getSessionStatus(sessionId));
    }
    /**
     * 세션 상태를 IN_PROGRESS로 변경
     */
    @PutMapping("/status/{sessionId}/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.updateSessionToInProgress(sessionId));
    }

    /**
     * 세션 상태를 COMPLETED로 변경
     */
    @PutMapping("/status/{sessionId}/complete")
    public ResponseEntity<Map<String, Object>> completeSession(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.updateSessionToCompleted(sessionId));
    }
    /**
     * middleReviewText 조회 - Map 직접 반환
     */
    @GetMapping("/middle-reviews/{sessionId}")
    public ResponseEntity<Map<String, String>> getMiddleReviewTexts(
            @PathVariable Integer sessionId) {
        return ResponseEntity.ok(interviewSessionService.getMiddleReviewTexts(sessionId));
    }
}