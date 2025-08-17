package com.skala03.skala_backend.entity.applicant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantKeywordScoreId implements Serializable {
    private String applicantId;
    private Integer keywordId;
}