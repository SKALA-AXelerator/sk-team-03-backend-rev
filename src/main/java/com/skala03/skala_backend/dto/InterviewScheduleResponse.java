package com.skala03.skala_backend.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduleResponse {
    private String interviewRoom;
    private String interviewTime;
    private List<ApplicantDto> applicantList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantDto {
        private String id;
        private String name;
    }
}