package com.skala03.skala_backend.service;

import com.skala03.skala_backend.client.FastApiClient;
import com.skala03.skala_backend.dto.InterviewProcessingDto;
import com.skala03.skala_backend.entity.*;
import com.skala03.skala_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewProcessingService {

    private final FastApiClient fastApiClient;
    private final SessionRepository sessionRepository;
    private final ApplicantRepository applicantRepository;
    private final AdminRepository adminRepository;
    private final KeywordRepository keywordRepository;
    private final ApplicantKeywordScoreRepository applicantKeywordScoreRepository;

    /**
     * 프론트엔드 요청을 처리하여 FastAPI 호출 후 DB 저장
     * 🔥 트랜잭션 제거 - 비동기 실행에서는 개별 메서드에서 트랜잭션 관리
     */
    public InterviewProcessingDto.ProcessingResponse processFullPipeline(
            InterviewProcessingDto.ProcessingRequest request) {

        try {
            log.info("🚀 면접 처리 시작: sessionId={}, jobRoleName={}, 지원자수={}",
                    request.getSessionId(), request.getJobRoleName(), request.getApplicantIds().size());

            // 1. 직무명으로 DB에서 평가기준 조회 (읽기 전용 트랜잭션)
            Map<String, Map<String, String>> evaluationCriteria = getEvaluationCriteriaByJobRole(request.getJobRoleName());

            if (evaluationCriteria.isEmpty()) {
                throw new RuntimeException("직무 '" + request.getJobRoleName() + "'에 대한 평가기준을 찾을 수 없습니다.");
            }

            log.info("✅ 평가기준 조회 완료: {}개 키워드", evaluationCriteria.size());

            // 2. FastAPI 요청 데이터 구성
            InterviewProcessingDto.FastApiRequest fastApiRequest = InterviewProcessingDto.FastApiRequest.builder()
                    .sessionId(request.getSessionId())
                    .applicantIds(request.getApplicantIds())
                    .applicantNames(request.getApplicantNames())
                    .jobRoleName(request.getJobRoleName())
                    .evaluationCriteria(evaluationCriteria)
                    .rawStt(request.getRawStt())
                    .build();

            // 3. FastAPI 호출 (트랜잭션 외부에서 실행)
            log.info("📡 FastAPI 호출 시작: sessionId={}", request.getSessionId());
            FastApiClient.FastApiPipelineResponse response = fastApiClient.callFullPipeline(fastApiRequest);

            if (!response.isSuccess()) {
                throw new RuntimeException("FastAPI 처리 실패: " + response.getMessage());
            }

            log.info("✅ FastAPI 처리 완료: 성공 {}명, 실패 {}명",
                    response.getSuccessfulCount(), response.getFailedCount());

            // 4. 응답 데이터를 DB에 저장 (개별 트랜잭션으로 처리)
            ProcessingResult result = saveProcessingResults(response);

            // 5. 응답 반환
            return InterviewProcessingDto.ProcessingResponse.builder()
                    .success(true)
                    .message(String.format("면접 처리가 완료되었습니다. (성공: %d명, 실패: %d명)",
                            result.getSuccessCount(), result.getFailureCount()))
                    .sessionId(response.getSessionId())
                    .totalProcessed(result.getTotalProcessed())
                    .successfulCount(result.getSuccessCount())
                    .failedCount(result.getFailureCount())
                    .totalProcessingTime(response.getTotalProcessingTime())
                    .build();

        } catch (Exception e) {
            log.error("❌ 면접 처리 실패: sessionId={}, error={}", request.getSessionId(), e.getMessage(), e);

            return InterviewProcessingDto.ProcessingResponse.builder()
                    .success(false)
                    .message("면접 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .sessionId(request.getSessionId())
                    .totalProcessed(0)
                    .successfulCount(0)
                    .failedCount(0)
                    .totalProcessingTime(0.0)
                    .build();
        }
    }

    /**
     * 직무명으로 DB에서 평가기준 조회 (읽기 전용 트랜잭션)
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> getEvaluationCriteriaByJobRole(String jobRoleName) {
        try {
            log.info("🔍 평가기준 조회 시작: jobRoleName={}", jobRoleName);

            List<Object[]> criteriaData = adminRepository.findEvaluationCriteriaByJobRoleName(jobRoleName);

            if (criteriaData.isEmpty()) {
                log.warn("⚠️ 평가기준이 없습니다: jobRoleName={}", jobRoleName);
                return Collections.emptyMap();
            }

            Map<String, Map<String, String>> evaluationCriteria = new LinkedHashMap<>();

            for (Object[] row : criteriaData) {
                String keywordName = (String) row[1];
                Integer score = (Integer) row[2];
                String guideline = (String) row[3];

                evaluationCriteria.computeIfAbsent(keywordName, k -> new LinkedHashMap<>());
                evaluationCriteria.get(keywordName).put(score.toString(), guideline);
            }

            log.info("✅ 평가기준 변환 완료: {}개 키워드, 총 {}개 기준",
                    evaluationCriteria.size(), criteriaData.size());

            return evaluationCriteria;

        } catch (Exception e) {
            log.error("❌ 평가기준 조회 실패: jobRoleName={}, error={}", jobRoleName, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * FastAPI 응답을 데이터베이스에 저장 (각각 독립적인 트랜잭션)
     */
    private ProcessingResult saveProcessingResults(FastApiClient.FastApiPipelineResponse response) {
        log.info("💾 DB 저장 시작: sessionId={}", response.getSessionId());

        ProcessingResult result = new ProcessingResult();

        try {
            // 1. 세션 정보 업데이트 (독립적인 트랜잭션)
            updateSessionStatus(response);
            log.info("✅ 세션 상태 업데이트 완료: sessionId={}", response.getSessionId());
        } catch (Exception e) {
            log.error("❌ 세션 상태 업데이트 실패: sessionId={}, error={}", response.getSessionId(), e.getMessage());
            // 세션 업데이트 실패해도 지원자 저장은 계속 진행
        }

        // 2. 지원자별 결과 저장 (각각 독립적인 트랜잭션)
        List<FastApiClient.ApplicantResult> results = response.getEvaluationResults();
        result.setTotalProcessed(results.size());

        for (FastApiClient.ApplicantResult applicantResult : results) {
            try {
                saveApplicantResult(applicantResult);
                result.incrementSuccess();
                log.info("✅ 지원자 저장 완료: applicantId={}", applicantResult.getApplicantId());
            } catch (Exception e) {
                result.incrementFailure();
                log.error("❌ 지원자 저장 실패: applicantId={}, error={}",
                        applicantResult.getApplicantId(), e.getMessage(), e);
            }
        }

        log.info("✅ DB 저장 완료: sessionId={}, 성공={}, 실패={}",
                response.getSessionId(), result.getSuccessCount(), result.getFailureCount());

        return result;
    }

    /**
     * 세션 상태 업데이트 (새로운 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSessionStatus(FastApiClient.FastApiPipelineResponse response) {
        Optional<Session> sessionOpt = sessionRepository.findById(response.getSessionId());
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setSessionStatus(Session.SessionStatus.COMPLETED);
            session.setRawDataPath(response.getRawSttS3Path());
            sessionRepository.save(session);

            log.debug("📝 세션 저장 완료: sessionId={}, status={}",
                    response.getSessionId(), session.getSessionStatus());
        } else {
            throw new RuntimeException("세션을 찾을 수 없습니다: " + response.getSessionId());
        }
    }

    /**
     * 개별 지원자 결과 저장 (새로운 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveApplicantResult(FastApiClient.ApplicantResult result) {
        // 1. 지원자 정보 업데이트
        Optional<Applicant> applicantOpt = applicantRepository.findById(result.getApplicantId());
        if (applicantOpt.isEmpty()) {
            throw new RuntimeException("지원자를 찾을 수 없습니다: " + result.getApplicantId());
        }

        Applicant applicant = applicantOpt.get();

        // 기본 정보 업데이트
        applicant.setInterviewStatus(InterviewStatus.COMPLETED);
        applicant.setCompletedAt(LocalDateTime.now());
        applicant.setIndividualPdfPath(result.getPdfS3Path());
        applicant.setIndividualQnaPath(result.getQnaS3Path());

        // 평가 정보 추출 및 저장
        updateApplicantEvaluationData(applicant, result.getEvaluationJson());

        applicantRepository.save(applicant);
        log.debug("📝 지원자 기본 정보 저장: applicantId={}", result.getApplicantId());

        // 2. 키워드별 점수 저장
        saveKeywordScores(result);
        log.debug("📝 키워드 점수 저장 완료: applicantId={}", result.getApplicantId());
    }

    /**
     * 지원자 평가 데이터 업데이트
     */
    private void updateApplicantEvaluationData(Applicant applicant, Map<String, Object> evaluationJson) {
        if (evaluationJson == null || evaluationJson.containsKey("error")) {
            log.warn("⚠️ 평가 데이터가 없거나 오류 포함: applicantId={}", applicant.getApplicantId());
            return;
        }

        try {
            // 총점 저장
            Object evaluationSummary = evaluationJson.get("evaluation_summary");
            if (evaluationSummary instanceof Map) {
                Map<String, Object> summary = (Map<String, Object>) evaluationSummary;
                Object totalScore = summary.get("total_score");
                if (totalScore instanceof Number) {
                    applicant.setTotalScore(((Number) totalScore).floatValue());
                    log.debug("📊 총점 저장: applicantId={}, score={}",
                            applicant.getApplicantId(), applicant.getTotalScore());
                }
            }

            // 면접 요약 저장
            Object interviewSummary = evaluationJson.get("interview_summary");
            if (interviewSummary instanceof String) {
                applicant.setTotalComment((String) interviewSummary);
            }

            // 추가 질문 저장
            Object nextQuestions = evaluationJson.get("next_questions");
            if (nextQuestions instanceof String) {
                applicant.setNextCheckpoint((String) nextQuestions);
            }

        } catch (Exception e) {
            log.error("❌ 평가 데이터 처리 실패: applicantId={}, error={}",
                    applicant.getApplicantId(), e.getMessage());
        }
    }

    /**
     * 키워드별 점수 저장 (호환성 개선)
     */
    private void saveKeywordScores(FastApiClient.ApplicantResult result) {
        try {
            Map<String, Object> evaluationJson = result.getEvaluationJson();
            if (evaluationJson == null || evaluationJson.containsKey("error")) {
                return;
            }

            Object detailedEvaluation = evaluationJson.get("detailed_evaluation");
            if (!(detailedEvaluation instanceof Map)) {
                return;
            }

            Map<String, Object> detailed = (Map<String, Object>) detailedEvaluation;
            List<ApplicantKeywordScore> keywordScores = new ArrayList<>();

            for (Map.Entry<String, Object> entry : detailed.entrySet()) {
                String keywordName = entry.getKey();
                Object keywordData = entry.getValue();

                if (!(keywordData instanceof Map)) {
                    continue;
                }

                Map<String, Object> keywordEval = (Map<String, Object>) keywordData;

                // 키워드 엔티티 조회 (개별 조회 - 호환성 보장)
                Optional<Keyword> keywordOpt = keywordRepository.findByKeywordName(keywordName);
                if (keywordOpt.isEmpty()) {
                    log.warn("⚠️ 키워드를 찾을 수 없습니다: {}", keywordName);
                    continue;
                }

                Keyword keyword = keywordOpt.get();
                Object finalScore = keywordEval.get("final_score");
                Object scoreRationale = keywordEval.get("score_rationale");

                if (finalScore instanceof Number) {
                    ApplicantKeywordScore score = new ApplicantKeywordScore();
                    score.setApplicantId(result.getApplicantId());
                    score.setKeywordId(keyword.getKeywordId());
                    score.setApplicantScore(((Number) finalScore).intValue());
                    score.setScoreComment(scoreRationale != null ? scoreRationale.toString() : "");

                    keywordScores.add(score);

                    log.debug("📊 키워드 점수 준비: {} - {}점", keywordName, score.getApplicantScore());
                }
            }

            // 일괄 저장
            if (!keywordScores.isEmpty()) {
                applicantKeywordScoreRepository.saveAll(keywordScores);
                log.debug("📊 키워드 점수 일괄 저장: applicantId={}, count={}",
                        result.getApplicantId(), keywordScores.size());
            } else {
                log.warn("⚠️ 저장할 키워드 점수가 없습니다: applicantId={}", result.getApplicantId());
            }

        } catch (Exception e) {
            log.error("❌ 키워드 점수 저장 실패: applicantId={}, error={}",
                    result.getApplicantId(), e.getMessage());
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 처리 결과를 추적하는 내부 클래스
     */
    private static class ProcessingResult {
        private int totalProcessed = 0;
        private int successCount = 0;
        private int failureCount = 0;

        public void incrementSuccess() { successCount++; }
        public void incrementFailure() { failureCount++; }

        // Getters and setters
        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
    }
}