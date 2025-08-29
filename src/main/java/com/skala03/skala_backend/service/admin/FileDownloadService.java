package com.skala03.skala_backend.service.admin;

import com.skala03.skala_backend.entity.applicant.Applicant;
import com.skala03.skala_backend.repository.applicant.ApplicantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadService {

    private final ApplicantRepository applicantRepository;


     // 특정 직무의 모든 지원자 PDF 파일들을 ZIP으로 압축

    public ByteArrayOutputStream createPdfZip(String jobRoleId) {
        List<Applicant> applicants = applicantRepository.findByJobRoleIdWithPdfPath(jobRoleId);

        if (applicants.isEmpty()) {
            throw new IllegalArgumentException("해당 직무에 PDF 파일이 있는 지원자가 없습니다.");
        }

        return createZipFromUrls(applicants, "pdf");
    }


     // 특정 직무의 모든 지원자 QNA 파일들을 ZIP으로 압축

    public ByteArrayOutputStream createQnaZip(String jobRoleId) {
        List<Applicant> applicants = applicantRepository.findByJobRoleIdWithQnaPath(jobRoleId);

        if (applicants.isEmpty()) {
            throw new IllegalArgumentException("해당 직무에 QNA 파일이 있는 지원자가 없습니다.");
        }

        return createZipFromUrls(applicants, "qna");
    }


     // presigned URL들로부터 파일을 다운로드하여 ZIP 파일 생성

    private ByteArrayOutputStream createZipFromUrls(List<Applicant> applicants, String fileType) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            for (Applicant applicant : applicants) {
                String fileUrl = getFileUrl(applicant, fileType);
                if (fileUrl == null || fileUrl.trim().isEmpty()) {
                    log.warn("지원자 {}의 {} 파일 URL이 없습니다.", applicant.getApplicantName(), fileType);
                    continue;
                }

                try {
                    downloadAndAddToZip(zipOut, fileUrl, applicant.getApplicantName(), applicant.getApplicantId(), fileType);
                } catch (Exception e) {
                    log.error("지원자 {}({})의 {} 파일 다운로드 실패: {}",
                            applicant.getApplicantName(), applicant.getApplicantId(), fileType, e.getMessage());
                    // 개별 파일 실패해도 계속 진행
                }
            }
        } catch (IOException e) {
            log.error("ZIP 파일 생성 중 오류 발생", e);
            throw new RuntimeException("ZIP 파일 생성에 실패했습니다.", e);
        }

        return baos;
    }


     // 파일 타입에 따라 적절한 URL 반환

    private String getFileUrl(Applicant applicant, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> applicant.getIndividualPdfPath();
            case "qna" -> applicant.getIndividualQnaPath();
            default -> throw new IllegalArgumentException("지원하지 않는 파일 타입: " + fileType);
        };
    }


     // presigned URL에서 파일을 다운로드하여 ZIP에 추가

    private void downloadAndAddToZip(ZipOutputStream zipOut, String fileUrl, String applicantName, String applicantId, String fileType)
            throws IOException {

        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000); // 10초 타임아웃
        connection.setReadTimeout(30000);    // 30초 타임아웃

        // 파일 확장자 결정
        String extension = fileType.equals("pdf") ? ".pdf" : ".txt";

        // 지원자명_지원자ID_파일타입.확장자 형태로 파일명 생성
        String fileName = sanitizeFileName(applicantName) + "_" + applicantId + "_" + fileType + extension;

        // ZIP 엔트리 생성
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        // 파일 다운로드 및 ZIP에 쓰기
        try (InputStream inputStream = connection.getInputStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                zipOut.write(buffer, 0, bytesRead);
            }
        } finally {
            zipOut.closeEntry();
        }

        log.info("파일 다운로드 완료: {}", fileName);
    }


     // 파일명에서 특수문자 제거

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }
}
