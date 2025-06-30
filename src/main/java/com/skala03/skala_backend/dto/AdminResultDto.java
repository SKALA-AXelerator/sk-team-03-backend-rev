// AdminResultDto.java
package com.skala03.skala_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResultDto {
    private String applicantId;
    private String applicantName;
    private String jobRoleName;
    private String interviewer;
    private String interviewDate;
    private String sessionTime;
    private String sessionLocation;
    private String interviewStatus;
    private Float totalScore;
    private List<KeywordScoreDto> applicantKeywordScores;
    private String individualPdfPath;
    private String individualQnaPath;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordScoreDto {
        private String keywordId;
        private String keywordName;
        private Integer applicantScore;
        private String scoreComment;
    }
}

