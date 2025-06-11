package com.skala03.skala_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_roles")
public class JobRole {
    @Id
    @Column(name = "job_role_id")
    private String jobRoleId;

    @Column(name = "job_role_name", nullable = false)
    private String jobRoleName;

    @Column(name = "total_excel_path")
    private String totalExcelPath;


    @OneToMany(mappedBy = "jobRole", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Applicant> applicants = new HashSet<>();
}