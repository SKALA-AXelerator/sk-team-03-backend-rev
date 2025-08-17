package com.skala03.skala_backend.entity.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCriteriaId implements Serializable {
    private Integer keywordId;
    private Integer keywordScore;
}