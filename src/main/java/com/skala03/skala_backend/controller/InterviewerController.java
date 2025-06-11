package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.global.common.ApiResponse;
import com.skala03.skala_backend.dto.InterviewerResponse;
import com.skala03.skala_backend.dto.InterviewScheduleRequest;
import com.skala03.skala_backend.dto.InterviewScheduleResponse;
import com.skala03.skala_backend.service.InterviewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviewers")
@Tag(name = "면접관 API", description = "면접관 기능 - 정보 확인")
public class InterviewerController {

    private final InterviewerService interviewerService;

    @GetMapping("/interviewer-text/{userId}")
    @Operation(summary = "면접관 참여 세션 조회", description = "면접관 ID로 참여 중인 세션의 통계 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<InterviewerResponse>> getInterviewerRooms(@PathVariable String userId) {
        InterviewerResponse response = interviewerService.getRoomsForInterviewer(userId);
        return ResponseEntity.ok(ApiResponse.success("면접관 세션 통계 조회 성공", response));
    }

    @PostMapping("/interview-schedule")
    @Operation(summary = "면접관 스케줄 목록 조회", description = "면접관 ID 리스트로 세션 시간 및 지원자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<InterviewScheduleResponse>>> getInterviewSchedules(
            @RequestBody InterviewScheduleRequest request) {
        List<InterviewScheduleResponse> schedules = interviewerService.getInterviewSchedules(request.getInterviewerIds());
        return ResponseEntity.ok(ApiResponse.success("면접 스케줄 조회 성공", schedules));
    }
}