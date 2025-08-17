package com.skala03.skala_backend.entity.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRoleKeywordId implements Serializable {
    private String jobRoleId;
    private Integer keywordId;
}