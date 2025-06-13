package com.skala03.skala_backend.controller;



import com.skala03.skala_backend.dto.InterviewSessionDto;
import com.skala03.skala_backend.service.InterviewSessionService;
import com.skala03.skala_backend.dto.InterviewSessionDto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/interviewers")
@CrossOrigin(origins = "*")
public class InterviewSessionController {

    @Autowired
    private InterviewSessionService interviewSessionService;

    // ✅ PathVariable 방식으로 수정됨
    @PostMapping("/enter-session-list/{roomId}/{userId}")
    public ResponseEntity<InterviewSessionDto.InterviewStatusResponse> enterSessionList(
            @PathVariable String roomId,
            @PathVariable String userId) {
        try {
            interviewSessionService.enterSessionList(roomId, userId);
            return ResponseEntity.ok(new InterviewSessionDto.InterviewStatusResponse(
                    true,
                    "세션 리스트에 성공적으로 입장했습니다.",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new InterviewSessionDto.InterviewStatusResponse(
                    false,
                    e.getMessage(),
                    null
            ));
        }
    }
    // ✅ PathVariable 방식으로 수정됨
    @PostMapping("/end-session/{roomId}/{userId}")
    public ResponseEntity<InterviewSessionDto.InterviewStatusResponse> endSession(
            @PathVariable String roomId,
            @PathVariable String userId) {
        try {
            interviewSessionService.endSession(roomId, userId);
            return ResponseEntity.ok(new InterviewSessionDto.InterviewStatusResponse(
                    true,
                    "면접이 성공적으로 종료되었습니다.",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new InterviewSessionDto.InterviewStatusResponse(
                    false,
                    e.getMessage(),
                    null
            ));
        }
    }

    /**
     * 참가자 상태 폴링 API
     */
    @GetMapping("/status/{roomId}/{userId}")
    public ResponseEntity<ParticipantStatusResponse> getParticipantStatus(
            @PathVariable String roomId,
            @PathVariable String userId) {
        try {
            ParticipantStatusResponse response =
                    interviewSessionService.getParticipantStatus(roomId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ParticipantStatusResponse(
                    null, null,  e.getMessage()
            ));
        }
    }

    /**
     * 면접 시작 API
     */
    @PostMapping("/start")
    public ResponseEntity<InterviewStatusResponse> startInterview(
            @Valid @RequestBody StartInterviewRequest request) {
        try {
            boolean success = interviewSessionService.startInterview(
                    request.getRoomId(),
                    request.getSessionId(),
                    request.getLeaderUserId()
            );

            return ResponseEntity.ok(new InterviewStatusResponse(
                    success,
                    success ? "면접이 시작되었습니다." : "모든 면접관이 대기 상태가 아닙니다.",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new InterviewStatusResponse(
                    false,
                    e.getMessage(),
                    null
            ));
        }
    }




}

