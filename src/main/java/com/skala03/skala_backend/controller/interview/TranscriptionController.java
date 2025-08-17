// ========================================
// 2. CONTROLLER LAYER - 웹 요청 처리
// ========================================

// TranscriptionController.java
package com.skala03.skala_backend.controller.interview;

import com.skala03.skala_backend.dto.interview.TranscriptionDto;
import com.skala03.skala_backend.service.interview.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/interviewers/transcription")
@Tag(name = "면접관 API")
@RequiredArgsConstructor
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    @PostMapping("/upload-audio")
    @Operation(summary = "음성 파일 업로드", description = "STT+화자 분리를 시작하고 transcription ID를 반환합니다.")
    public ResponseEntity<TranscriptionDto.StartResponse> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "speakerCount", required = false) Integer speakerCount) {

        try {
            String transcriptionId = transcriptionService.startTranscription(file, speakerCount);

            TranscriptionDto.StartResponse response = TranscriptionDto.StartResponse.builder()
                    .transcriptionId(transcriptionId)
                    .status("processing")
                    .message("전사 작업이 시작되었습니다.")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("파일 업로드 실패", e);

            TranscriptionDto.StartResponse errorResponse = TranscriptionDto.StartResponse.builder()
                    .status("failed")
                    .message("파일 업로드에 실패했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{transcriptionId}")
    @Operation(summary = "전사 상태 및 결과 조회", description = "transcription ID로 전사 상태를 확인하고, 완료시 결과를 반환합니다.")
    public ResponseEntity<TranscriptionDto.StatusResponse> getTranscription(
            @PathVariable String transcriptionId) {

        String status = transcriptionService.getTranscriptionStatus(transcriptionId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        TranscriptionDto.StatusResponse.StatusResponseBuilder responseBuilder =
                TranscriptionDto.StatusResponse.builder()
                        .transcriptionId(transcriptionId)
                        .status(status);

        switch (status) {
            case "processing" -> {
                responseBuilder.message("전사 작업이 진행 중입니다.");
            }
            case "completed" -> {
                var result = transcriptionService.getTranscriptionResult(transcriptionId);

                // utterances만 추출해서 간단한 구조로 변환
                List<TranscriptionDto.Utterance> utterances = null;
                if (result != null && result.getResults() != null && result.getResults().getUtterances() != null) {
                    utterances = result.getResults().getUtterances().stream()
                            .map(vitoUtterance -> TranscriptionDto.Utterance.builder()
                                    .spk(vitoUtterance.getSpk())
                                    .msg(vitoUtterance.getMsg())
                                    .build())
                            .collect(java.util.stream.Collectors.toList());
                }

                responseBuilder
                        .message("전사 작업이 완료되었습니다.")
                        .result(utterances);
            }
            case "failed" -> {
                String errorMessage = transcriptionService.getTranscriptionError(transcriptionId);
                responseBuilder
                        .message("전사 작업이 실패했습니다.")
                        .error(errorMessage != null ? errorMessage : "알 수 없는 오류가 발생했습니다.");
            }
            default -> {
                responseBuilder.message("알 수 없는 상태입니다.");
            }
        }

        return ResponseEntity.ok(responseBuilder.build());
    }
}