package com.skala03.skala_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_role_keywords")
@IdClass(JobRoleKeywordId.class)
public class JobRoleKeyword {
    @Id
    @Column(name = "job_role_id")
    private String jobRoleId;

    @Id
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "selected")
    private Boolean selected;


}