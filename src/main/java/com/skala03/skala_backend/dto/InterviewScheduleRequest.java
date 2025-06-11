package com.skala03.skala_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class InterviewScheduleRequest {
    private List<String> interviewerIds;
}