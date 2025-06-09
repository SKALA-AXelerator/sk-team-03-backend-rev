package com.skala03.skala_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantQuestionId implements Serializable {
    private String applicantId;
    private Integer questionId;
}