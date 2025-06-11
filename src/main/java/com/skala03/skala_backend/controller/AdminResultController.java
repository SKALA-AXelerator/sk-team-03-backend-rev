package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.AdminResultDto;
import com.skala03.skala_backend.service.AdminResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResultController {

    private final AdminResultService adminResultService;

    /**
     * 특정 직군의 지원자 평가 이력 조회
     * @param jobRoleId 직군 ID (예: role-frontend-01)
     * @return 지원자 평가 이력 리스트
     */
    @GetMapping("/results/{job_role_id}")
    public ResponseEntity<List<AdminResultDto>> getJobRoleResults(
            @PathVariable("job_role_id") String jobRoleId) {

        try {
            List<AdminResultDto> results = adminResultService.getJobRoleResults(jobRoleId);

            if (results.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}