package com.skala03.skala_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// JobRole Entity
@Entity
@Getter
@NoArgsConstructor
@Table(name = "job_roles")
public class JobRole {

    @Id
    @Column(name = "job_role_id", nullable = false)
    private String jobRoleId;

    @Column(name = "job_role_name", nullable = false)
    private String jobRoleName;

    @Column(name = "total_summary_path", columnDefinition = "TEXT")
    private String totalSummaryPath;

    @Column(name = "total_excel_path", columnDefinition = "TEXT")
    private String totalExcelPath;
}
