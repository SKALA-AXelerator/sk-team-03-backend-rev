package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.InterviewerResponse;
import com.skala03.skala_backend.dto.InterviewScheduleResponse;
import com.skala03.skala_backend.service.InterviewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.skala03.skala_backend.dto.ParticipantStatusUpdateRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviewers")
@Tag(name = "면접관 API", description = "면접관 기능 - 정보 확인")
public class InterviewerController {

    private final InterviewerService interviewerService;

    @GetMapping("/interviewer-text/{userId}")
    @Operation(summary = "면접관 참여 룸 조회", description = "면접관 ID로 참여 중인 룸의 통계 정보를 조회합니다.")
    public ResponseEntity<InterviewerResponse> getInterviewerRooms(@PathVariable String userId) {
        InterviewerResponse response = interviewerService.getRoomsForInterviewer(userId);
        return ResponseEntity.ok(response);
    }

    // ✅ 기존 DTO 재사용 - InterviewScheduleResponse
    @GetMapping("/room-sessions/{roomId}")
    @Operation(summary = "룸별 세션 목록 조회", description = "룸 ID로 해당 룸의 모든 세션과 지원자 정보를 조회합니다.")
    public ResponseEntity<List<InterviewScheduleResponse>> getRoomSessions(@PathVariable String roomId) {
        List<InterviewScheduleResponse> sessions = interviewerService.getRoomSessions(roomId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * ✅ 면접관 상태 변경 (단순 버전) - 이 메서드를 맨 마지막에 추가하세요
     */
    @PutMapping("/status/{userId}")
    @Operation(summary = "면접관 상태 변경", description = "면접관 ID로 상태를 변경합니다. (OFFLINE, WAITING, IN_PROGRESS)")
    public ResponseEntity<String> updateInterviewerStatus(
            @PathVariable String userId,
            @RequestBody ParticipantStatusUpdateRequest request) {

        String message = interviewerService.updateInterviewerStatus(userId, request.getStatus());
        return ResponseEntity.ok(message);
    }
}