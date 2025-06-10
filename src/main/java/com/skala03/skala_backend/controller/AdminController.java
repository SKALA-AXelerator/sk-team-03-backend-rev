package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.dto.AdminDto;
import com.skala03.skala_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "관리자 API", description = "관리자 기능 - 키워드 관리")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // 1. 키워드 목록 조회
    @GetMapping("/keywords")
    @Operation(summary = "키워드 이름, 키워드 설명, 키워드 평가기준 리스트 조회")
    public ResponseEntity<List<AdminDto.KeywordResponse>> getAllKeywords() {
        try {
            List<AdminDto.KeywordResponse> response = adminService.getAllKeywords();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. 키워드 생성
    @PostMapping("/create-keywords")
    @Operation(summary = "키워드 및 평가 기준 생성 (AI x)")
    public ResponseEntity<String> createKeyword(@RequestBody AdminDto.CreateKeywordRequest request) {
        try {
            String message = adminService.createKeyword(request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    // 3. AI 기반 평가기준 생성
    @PostMapping("/ai-generate-keywords/{keywordId}")
    @Operation(summary = "특정 키워드에 대한 평가기준을 AI로 생성")
    public ResponseEntity<?> aiGenerateKeywords(
            @PathVariable Integer keywordId,
            @RequestBody AdminDto.AiGenerateRequest request) {
        try {
            AdminDto.AiGenerateResponse response = adminService.generateAiCriteria(keywordId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    // 4. 키워드 수정
    @PutMapping("/change-keywords/{keywordId}")
    @Operation(summary = "키워드에 대한 설명 및 평가 기준 수정")
    public ResponseEntity<String> updateKeyword(
            @PathVariable Integer keywordId,
            @RequestBody AdminDto.UpdateKeywordRequest request) {
        try {
            String message = adminService.updateKeyword(keywordId, request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    // 5. 키워드 삭제
    @DeleteMapping("/delete-keywords/{keywordId}")
    @Operation(summary = "특정 키워드 및 평가 기준 삭제")
    public ResponseEntity<String> deleteKeyword(@PathVariable Integer keywordId) {
        try {
            String message = adminService.deleteKeyword(keywordId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    // 전체 직군 및 매핑된 키워드 + 평가 기준 조회
    @GetMapping("/job-roles-keyword")
    @Operation(summary = "전체 직군 및 매핑된 키워드 + 평가 기준 조회")
    public ResponseEntity<List<AdminDto.JobRoleKeywordResponse>> getAllJobRolesWithKeywords() {
        try {
            List<AdminDto.JobRoleKeywordResponse> response = adminService.getAllJobRolesWithKeywords();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 전체 직군 조회
    @GetMapping("/job-roles")
    @Operation(summary = "전체 직군 조회")
    public ResponseEntity<List<AdminDto.JobRoleResponse>> getAllJobRoles() {
        try {
            List<AdminDto.JobRoleResponse> response = adminService.getAllJobRoles();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 직군에 대한 키워드 선택 상태 수정
    @PutMapping("/job-roles/{jobRoleId}/keywords")
    @Operation(summary = "특정 직군에 대한 키워드 선택 상태(selected) 수정")
    public ResponseEntity<?> updateJobRoleKeywords(
            @PathVariable String jobRoleId,
            @RequestBody AdminDto.UpdateKeywordSelectionRequest request) {
        try {
            AdminDto.UpdateKeywordSelectionResponse response = adminService.updateJobRoleKeywords(jobRoleId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }
}