package com.skala03.skala_backend.controller;

import com.skala03.skala_backend.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Slf4j
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    /**
     * 특정 직무의 모든 지원자 PDF 파일들을 ZIP으로 다운로드
     */
    @GetMapping("/download-pdf-zip/{jobRoleId}")
    public ResponseEntity<ByteArrayResource> downloadPdfZip(@PathVariable String jobRoleId) {
        log.info("PDF ZIP 다운로드 요청 - 직무 ID: {}", jobRoleId);

        try {
            ByteArrayOutputStream zipStream = fileDownloadService.createPdfZip(jobRoleId);
            ByteArrayResource resource = new ByteArrayResource(zipStream.toByteArray());

            String fileName = "job_role_" + jobRoleId + "_pdf_files.zip";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (IllegalArgumentException e) {
            log.warn("PDF ZIP 다운로드 실패 - 직무 ID: {}, 오류: {}", jobRoleId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("PDF ZIP 다운로드 중 오류 발생 - 직무 ID: {}", jobRoleId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 직무의 모든 지원자 QNA 파일들을 ZIP으로 다운로드
     */
    @GetMapping("/download-qna-zip/{jobRoleId}")
    public ResponseEntity<ByteArrayResource> downloadQnaZip(@PathVariable String jobRoleId) {
        log.info("QNA ZIP 다운로드 요청 - 직무 ID: {}", jobRoleId);

        try {
            ByteArrayOutputStream zipStream = fileDownloadService.createQnaZip(jobRoleId);
            ByteArrayResource resource = new ByteArrayResource(zipStream.toByteArray());

            String fileName = "job_role_" + jobRoleId + "_qna_files.zip";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (IllegalArgumentException e) {
            log.warn("QNA ZIP 다운로드 실패 - 직무 ID: {}, 오류: {}", jobRoleId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("QNA ZIP 다운로드 중 오류 발생 - 직무 ID: {}", jobRoleId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}