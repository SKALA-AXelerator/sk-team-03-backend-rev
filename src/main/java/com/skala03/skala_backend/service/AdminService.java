package com.skala03.skala_backend.service;

import com.skala03.skala_backend.client.FastApiClient;
import com.skala03.skala_backend.dto.AdminDto;
import com.skala03.skala_backend.entity.Keyword;
import com.skala03.skala_backend.entity.KeywordCriteria;
import com.skala03.skala_backend.entity.JobRole;
import com.skala03.skala_backend.entity.JobRoleKeyword;
import com.skala03.skala_backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FastApiClient fastApiClient;

    // 1. 키워드 목록 조회
    @Transactional(readOnly = true)
    public List<AdminDto.KeywordResponse> getAllKeywords() {
        List<Keyword> keywords = adminRepository.findAll();

        return keywords.stream()
                .map(keyword -> {
                    List<KeywordCriteria> criteria = adminRepository.findCriteriaByKeywordId(keyword.getKeywordId());

                    List<AdminDto.KeywordCriteriaInfo> criteriaInfos = criteria.stream()
                            .map(c -> new AdminDto.KeywordCriteriaInfo(c.getKeywordScore(), c.getKeywordGuideline()))
                            .collect(Collectors.toList());

                    return new AdminDto.KeywordResponse(
                            keyword.getKeywordId(),
                            keyword.getKeywordName(),
                            keyword.getKeywordDetail(),
                            criteriaInfos
                    );
                })
                .collect(Collectors.toList());
    }

    // 2. 키워드 생성
    public String createKeyword(AdminDto.CreateKeywordRequest request) {
        // 중복 체크
        if (adminRepository.existsByKeywordName(request.getKeywordName())) {
            throw new RuntimeException("Keyword already exists: " + request.getKeywordName());
        }

        // 키워드 저장
        Keyword keyword = new Keyword();
        keyword.setKeywordName(request.getKeywordName());
        keyword.setKeywordDetail(request.getKeywordDetail());
        Keyword savedKeyword = adminRepository.save(keyword);

        // 평가기준 저장
        for (AdminDto.KeywordCriteriaInfo criteria : request.getKeywordCriteria()) {
            adminRepository.insertCriteria(
                    savedKeyword.getKeywordId(),
                    criteria.getKeywordScore(),
                    criteria.getKeywordGuideline()
            );
        }

        return "Keyword created successfully";
    }

    // 3. AI 기반 평가기준 생성 (기존 키워드에 대한 평가기준 생성)
    public AdminDto.AiGenerateResponse generateAiCriteria(Integer keywordId, AdminDto.AiGenerateRequest request) {

        // 키워드 존재 여부 확인
        Keyword keyword = adminRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found: " + keywordId));

        try {
            // FastAPI 헬스체크 먼저 수행
            if (!fastApiClient.isHealthy()) {
                log.warn("FastAPI 서버 헬스체크 실패, mock 데이터로 대체");
                return generateMockResponse(keywordId, keyword, "FastAPI 서버에 연결할 수 없어 기본 평가기준을 생성했습니다.");
            }

            // FastAPI 요청 구성 - DB에서 조회한 키워드 정보 사용
            FastApiClient.FastApiRequest fastApiRequest = FastApiClient.FastApiRequest.builder()
                    .keywordName(keyword.getKeywordName()) // DB에서 조회
                    .keywordDetail(keyword.getKeywordDetail() != null ? keyword.getKeywordDetail() : "")
                    .build(); // keyword_id 제거

            log.debug("FastAPI 요청 데이터: keywordName={}", fastApiRequest.getKeywordName());

            // FastAPI 호출
            FastApiClient.FastApiResponse fastApiResponse = fastApiClient.generateKeywordCriteria(fastApiRequest);

            if (fastApiResponse == null) {
                log.error("FastAPI 응답이 null: keywordId={}", keywordId);
                return generateMockResponse(keywordId, keyword, "AI 서비스 응답이 없어 기본 평가기준을 생성했습니다.");
            }

            if (!fastApiResponse.isSuccess()) {
                String errorMsg = fastApiResponse.getErrorDetail() != null ? fastApiResponse.getErrorDetail() : "알 수 없는 오류";
                log.error("AI 키워드 생성 실패: keywordId={}, error={}", keywordId, errorMsg);
                return generateMockResponse(keywordId, keyword, "AI 분석 실패: " + errorMsg);
            }

            if (fastApiResponse.getCriteria() == null || fastApiResponse.getCriteria().isEmpty()) {
                log.error("FastAPI 응답에 평가기준이 없음: keywordId={}", keywordId);
                return generateMockResponse(keywordId, keyword, "AI가 평가기준을 생성하지 못해 기본 평가기준을 생성했습니다.");
            }

            // FastAPI 응답을 AdminDto로 변환
            List<AdminDto.KeywordCriteriaInfo> aiCriteria = fastApiResponse.getCriteria().entrySet().stream()
                    .map(entry -> new AdminDto.KeywordCriteriaInfo(entry.getKey(), entry.getValue()))
                    .sorted((a, b) -> b.getKeywordScore() - a.getKeywordScore()) // 5점부터 1점까지 정렬
                    .collect(Collectors.toList());

            log.info("AI 생성된 기준 수: {}", aiCriteria.size());

            // 기존 평가기준 삭제 후 새로 추가
            adminRepository.deleteCriteriaByKeywordId(keywordId);

            for (AdminDto.KeywordCriteriaInfo criteria : aiCriteria) {
                adminRepository.insertCriteria(keywordId, criteria.getKeywordScore(), criteria.getKeywordGuideline());
            }

            log.info("AI 키워드 평가 기준 생성 완료: keywordId={}", keywordId);
            return new AdminDto.AiGenerateResponse(aiCriteria, "AI 기반 평가기준이 성공적으로 생성되었습니다.");

        } catch (Exception e) {
            log.error("AI 키워드 생성 중 예외 발생: keywordId={}", keywordId, e);
            return generateMockResponse(keywordId, keyword, "AI 서비스 일시적 오류로 기본 평가기준을 생성했습니다: " + e.getMessage());
        }
    }

    // 새로운 키워드용 (아직 DB에 없는 키워드) - 새로 추가
    public AdminDto.AiGenerateResponse generateAiCriteriaForNewKeyword(String keywordName, String keywordDetail) {

        try {
            // FastAPI 헬스체크
            if (!fastApiClient.isHealthy()) {
                log.warn("FastAPI 서버 헬스체크 실패");
                return generateMockResponseForNewKeyword(keywordName, keywordDetail, "FastAPI 서버 연결 실패");
            }

            // FastAPI 요청 구성 - ID 없이 이름과 설명만
            FastApiClient.FastApiRequest fastApiRequest = FastApiClient.FastApiRequest.builder()
                    .keywordName(keywordName)
                    .keywordDetail(keywordDetail != null ? keywordDetail : "")
                    .build();

            log.debug("FastAPI 요청 데이터: keywordName={}", fastApiRequest.getKeywordName());

            // FastAPI 호출
            FastApiClient.FastApiResponse fastApiResponse = fastApiClient.generateKeywordCriteria(fastApiRequest);

            // 응답 처리 로직은 기존과 동일...
            if (fastApiResponse != null && fastApiResponse.isSuccess() && fastApiResponse.getCriteria() != null) {
                List<AdminDto.KeywordCriteriaInfo> aiCriteria = fastApiResponse.getCriteria().entrySet().stream()
                        .map(entry -> new AdminDto.KeywordCriteriaInfo(entry.getKey(), entry.getValue()))
                        .sorted((a, b) -> b.getKeywordScore() - a.getKeywordScore())
                        .collect(Collectors.toList());

                return new AdminDto.AiGenerateResponse(aiCriteria, "AI 기반 평가기준이 성공적으로 생성되었습니다.");
            } else {
                return generateMockResponseForNewKeyword(keywordName, keywordDetail, "AI 응답 처리 실패");
            }

        } catch (Exception e) {
            log.error("신규 키워드 AI 생성 중 예외: keywordName={}", keywordName, e);
            return generateMockResponseForNewKeyword(keywordName, keywordDetail, "AI 서비스 오류: " + e.getMessage());
        }
    }

    // Mock 응답 생성 메서드 (fallback용) - 기존
    private AdminDto.AiGenerateResponse generateMockResponse(Integer keywordId, Keyword keyword, String message) {
        log.warn("Mock 데이터 생성: keywordId={}, message={}", keywordId, message);

        List<AdminDto.KeywordCriteriaInfo> mockCriteria = generateMockCriteria(keyword.getKeywordName(), keyword.getKeywordDetail());

        // 기존 평가기준 삭제 후 mock 데이터 추가
        adminRepository.deleteCriteriaByKeywordId(keywordId);
        for (AdminDto.KeywordCriteriaInfo criteria : mockCriteria) {
            adminRepository.insertCriteria(keywordId, criteria.getKeywordScore(), criteria.getKeywordGuideline());
        }

        return new AdminDto.AiGenerateResponse(mockCriteria, message);
    }

    // Mock 응답 생성 메서드 (신규 키워드용) - 새로 추가
    private AdminDto.AiGenerateResponse generateMockResponseForNewKeyword(String keywordName, String keywordDetail, String message) {
        log.warn("신규 키워드 Mock 데이터 생성: keywordName={}, message={}", keywordName, message);

        List<AdminDto.KeywordCriteriaInfo> mockCriteria = generateMockCriteria(keywordName, keywordDetail);

        // 신규 키워드는 DB에 저장하지 않고 응답만 반환
        return new AdminDto.AiGenerateResponse(mockCriteria, message);
    }


    // 4. 키워드 수정
    public String updateKeyword(Integer keywordId, AdminDto.UpdateKeywordRequest request) {
        // 키워드 존재 여부 확인
        Keyword keyword = adminRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found: " + keywordId));

        // 키워드 상세 정보 업데이트
        keyword.setKeywordDetail(request.getKeywordDetail());
        adminRepository.save(keyword);

        // 기존 평가기준 삭제 후 새로 추가
        adminRepository.deleteCriteriaByKeywordId(keywordId);

        for (AdminDto.KeywordCriteriaInfo criteria : request.getKeywordCriteria()) {
            adminRepository.insertCriteria(keywordId, criteria.getKeywordScore(), criteria.getKeywordGuideline());
        }

        return "Keyword updated successfully";
    }

    // 5. 키워드 삭제
    public String deleteKeyword(Integer keywordId) {
        // 키워드 존재 여부 확인
        if (!adminRepository.existsById(keywordId)) {
            throw new RuntimeException("Keyword not found: " + keywordId);
        }

        // 평가기준 먼저 삭제 (외래키 때문에)
        adminRepository.deleteCriteriaByKeywordId(keywordId);

        // 키워드 삭제
        adminRepository.deleteById(keywordId);

        return "Keyword deleted successfully";
    }

    // 임시 mock 데이터 생성 메서드
    private List<AdminDto.KeywordCriteriaInfo> generateMockCriteria(String keywordName, String keywordDetail) {
        return Arrays.asList(
                new AdminDto.KeywordCriteriaInfo(5, "최고 수준의 " + keywordName + " 능력을 보여주며 구체적 사례 제시"),
                new AdminDto.KeywordCriteriaInfo(4, "우수한 " + keywordName + " 능력을 보여주며 경험 기반 설명"),
                new AdminDto.KeywordCriteriaInfo(3, "보통 수준의 " + keywordName + " 능력이 있으나 구체성 부족"),
                new AdminDto.KeywordCriteriaInfo(2, keywordName + "에 대한 이해는 있으나 실행 사례 부족"),
                new AdminDto.KeywordCriteriaInfo(1, keywordName + " 능력이 부족하거나 소극적 태도")
        );
    }

    // === 새로 추가: JobRole 관련 메서드들 ===

    // 1. 전체 직군 및 매핑된 키워드 + 평가 기준 조회
    @Transactional(readOnly = true)
    public List<AdminDto.JobRoleKeywordResponse> getAllJobRolesWithKeywords() {
        List<JobRole> jobRoles = adminRepository.findAllJobRoles();
        List<Keyword> allKeywords = adminRepository.findAllKeywords();
        List<JobRoleKeyword> allJobRoleKeywords = adminRepository.findAllJobRoleKeywords();

        // 키워드별 매핑 정보를 Map으로 구성 (성능 최적화)
        Map<String, Map<Integer, Boolean>> jobRoleKeywordMap = allJobRoleKeywords.stream()
                .collect(Collectors.groupingBy(
                        JobRoleKeyword::getJobRoleId,
                        Collectors.toMap(
                                JobRoleKeyword::getKeywordId,
                                JobRoleKeyword::getSelected
                        )
                ));

        return jobRoles.stream()
                .map(jobRole -> {
                    List<AdminDto.KeywordWithCriteriaInfo> keywordsWithCriteria = allKeywords.stream()
                            .map(keyword -> {
                                // 해당 직군-키워드 매핑 정보 확인
                                Map<Integer, Boolean> keywordMap = jobRoleKeywordMap.getOrDefault(jobRole.getJobRoleId(), new HashMap<>());
                                Boolean isSelected = keywordMap.getOrDefault(keyword.getKeywordId(), false);

                                // 키워드 평가 기준 조회
                                List<KeywordCriteria> criteria = adminRepository.findCriteriaByKeywordId(keyword.getKeywordId());
                                List<AdminDto.KeywordCriteriaInfo> criteriaInfos = criteria.stream()
                                        .map(c -> new AdminDto.KeywordCriteriaInfo(c.getKeywordScore(), c.getKeywordGuideline()))
                                        .collect(Collectors.toList());

                                return new AdminDto.KeywordWithCriteriaInfo(
                                        keyword.getKeywordId(),
                                        keyword.getKeywordName(), // keyword_title
                                        keyword.getKeywordDetail(),
                                        isSelected,
                                        criteriaInfos
                                );
                            })
                            .collect(Collectors.toList());

                    return new AdminDto.JobRoleKeywordResponse(
                            jobRole.getJobRoleId(),
                            jobRole.getJobRoleName(),
                            keywordsWithCriteria
                    );
                })
                .collect(Collectors.toList());
    }

    // 2. 전체 직군 조회
    @Transactional(readOnly = true)
    public List<AdminDto.JobRoleResponse> getAllJobRoles() {
        List<JobRole> jobRoles = adminRepository.findAllJobRoles();

        return jobRoles.stream()
                .map(jobRole -> new AdminDto.JobRoleResponse(
                        jobRole.getJobRoleId(),
                        jobRole.getJobRoleName()
                ))
                .collect(Collectors.toList());
    }

    // 3. 특정 직군에 대한 키워드 선택 상태 수정
    @Transactional
    public AdminDto.UpdateKeywordSelectionResponse updateJobRoleKeywords(
            String jobRoleId, AdminDto.UpdateKeywordSelectionRequest request) {

        // 직군 존재 여부 확인 (job_roles 테이블에서)
        List<JobRole> jobRoles = adminRepository.findAllJobRoles();
        boolean jobRoleExists = jobRoles.stream()
                .anyMatch(jr -> jr.getJobRoleId().equals(jobRoleId));

        if (!jobRoleExists) {
            throw new RuntimeException("Job role not found: " + jobRoleId);
        }

        // 키워드 선택 상태 업데이트
        List<AdminDto.KeywordSelectionInfo> updatedKeywords = new ArrayList<>();

        for (AdminDto.KeywordSelectionInfo keywordInfo : request.getKeywords()) {
            adminRepository.upsertJobRoleKeyword(
                    jobRoleId,
                    keywordInfo.getKeywordId(),
                    keywordInfo.getSelected()
            );

            updatedKeywords.add(new AdminDto.KeywordSelectionInfo(
                    keywordInfo.getKeywordId(),
                    keywordInfo.getSelected()
            ));
        }

        return new AdminDto.UpdateKeywordSelectionResponse(
                jobRoleId,
                "Job role keywords updated successfully",
                updatedKeywords
        );
    }
}