// ========================================
// 1. DTO LAYER - 데이터 전송 객체
// ========================================

// TranscriptionDto.java - 요청/응답 DTO
package com.skala03.skala_backend.dto;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public class TranscriptionDto {

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StartResponse {
        private String transcriptionId;
        private String status;
        private String message;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusResponse {
        private String transcriptionId;
        private String status;
        private String message;
        private List<Utterance> result; // utterances 리스트만 직접 포함
        private String error; // 실패시에만 포함
    }

    @Data
    @Builder
    public static class Utterance {
        private Integer spk;  // 화자 ID (0부터 시작)
        private String msg;   // 발화 내용
    }
}