package com.skala03.skala_backend.dto.interview;

import com.skala03.skala_backend.entity.interview.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduleResponse {
    private Integer sessionId;         // ✅ sessionId 추가
    private String interviewRoom;
    private String interviewTime;
    private Session.SessionStatus sessionStatus;  // ✅ 추가: 세션 상태
    private List<ApplicantDto> applicantList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantDto {
        private String id;
        private String name;
    }
}