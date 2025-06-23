package com.skala03.skala_backend.service;

import com.skala03.skala_backend.client.FastApiClient;  // ← 변경
import com.skala03.skala_backend.dto.InterviewProcessingDto;
import com.skala03.skala_backend.entity.*;
import com.skala03.skala_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InterviewProcessingService {

    private final FastApiClient fastApiClient;  // ← 변경
    private final SessionRepository sessionRepository;
    private final ApplicantRepository applicantRepository;
    private final AdminRepository adminRepository;
    private final KeywordRepository keywordRepository;
    private final ApplicantKeywordScoreRepository applicantKeywordScoreRepository;

    /**
     * 프론트엔드 요청을 처리하여 FastAPI 호출 후 DB 저장
     */
    public InterviewProcessingDto.ProcessingResponse processFullPipeline(
            InterviewProcessingDto.ProcessingRequest request) {

        try {
            log.info("🚀 면접 처리 시작: sessionId={}, jobRoleName={}, 지원자수={}",
                    request.getSessionId(), request.getJobRoleName(), request.getApplicantIds().size());

            // 1. 직무명으로 DB에서 평가기준 조회
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
                    .evaluationCriteria(evaluationCriteria)  // DB에서 조회한 평가기준
                    .rawStt(request.getRawStt())
                    .build();

            // 3. FastAPI 호출
            FastApiClient.FastApiPipelineResponse response = fastApiClient.callFullPipeline(fastApiRequest);  // ← 변경

            if (!response.isSuccess()) {
                throw new RuntimeException("FastAPI 처리 실패: " + response.getMessage());
            }

            log.info("✅ FastAPI 처리 완료: 성공 {}명, 실패 {}명",
                    response.getSuccessfulCount(), response.getFailedCount());

            // 4. 응답 데이터를 DB에 저장
            saveProcessingResults(response);

            // 5. 응답 반환
            return InterviewProcessingDto.ProcessingResponse.builder()
                    .success(true)
                    .message("면접 처리가 완료되었습니다.")
                    .sessionId(response.getSessionId())
                    .totalProcessed(response.getTotalProcessed())
                    .successfulCount(response.getSuccessfulCount())
                    .failedCount(response.getFailedCount())
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
     * 직무명으로 DB에서 평가기준 조회
     */
    private Map<String, Map<String, String>> getEvaluationCriteriaByJobRole(String jobRoleName) {
        try {
            log.info("🔍 평가기준 조회 시작: jobRoleName={}", jobRoleName);

            // DB에서 직무별 평가기준 조회
            List<Object[]> criteriaData = adminRepository.findEvaluationCriteriaByJobRoleName(jobRoleName);

            if (criteriaData.isEmpty()) {
                log.warn("⚠️ 평가기준이 없습니다: jobRoleName={}", jobRoleName);
                return Collections.emptyMap();
            }

            // 데이터를 Map<키워드명, Map<점수, 가이드라인>> 형태로 변환
            Map<String, Map<String, String>> evaluationCriteria = new LinkedHashMap<>();

            for (Object[] row : criteriaData) {
                String keywordName = (String) row[1];      // k.keyword_name
                Integer score = (Integer) row[2];          // kc.keyword_score
                String guideline = (String) row[3];        // kc.keyword_guideline

                // 키워드별 Map이 없으면 생성
                evaluationCriteria.computeIfAbsent(keywordName, k -> new LinkedHashMap<>());

                // 점수를 String으로 변환하여 저장 ("1", "2", "3", "4", "5")
                evaluationCriteria.get(keywordName).put(score.toString(), guideline);
            }

            log.info("✅ 평가기준 변환 완료: {}개 키워드, 총 {}개 기준",
                    evaluationCriteria.size(), criteriaData.size());

            // 로그로 구조 확인 (디버깅용)
            evaluationCriteria.forEach((keyword, criteria) -> {
                log.debug("키워드: {}, 기준수: {}", keyword, criteria.size());
            });

            return evaluationCriteria;

        } catch (Exception e) {
            log.error("❌ 평가기준 조회 실패: jobRoleName={}, error={}", jobRoleName, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * FastAPI 응답을 데이터베이스에 저장
     */
    private void saveProcessingResults(FastApiClient.FastApiPipelineResponse response) {  // ← 변경
        try {
            log.info("💾 DB 저장 시작: sessionId={}", response.getSessionId());

            // 1. 세션 정보 업데이트
            updateSessionStatus(response);

            // 2. 지원자별 결과 저장
            for (FastApiClient.ApplicantResult result : response.getEvaluationResults()) {  // ← 변경
                saveApplicantResult(result);
            }

            log.info("✅ DB 저장 완료: sessionId={}", response.getSessionId());

        } catch (Exception e) {
            log.error("❌ DB 저장 실패: sessionId={}, error={}", response.getSessionId(), e.getMessage(), e);
            throw new RuntimeException("DB 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 세션 상태 업데이트
     */
    private void updateSessionStatus(FastApiClient.FastApiPipelineResponse response) {  // ← 변경
        try {
            Optional<Session> sessionOpt = sessionRepository.findById(response.getSessionId());
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                session.setSessionStatus(Session.SessionStatus.COMPLETED);
                session.setRawDataPath(response.getRawSttS3Path());
                sessionRepository.save(session);

                log.info("✅ 세션 상태 업데이트: sessionId={}", response.getSessionId());
            }
        } catch (Exception e) {
            log.error("❌ 세션 업데이트 실패: sessionId={}, error={}", response.getSessionId(), e.getMessage());
        }
    }

    /**
     * 개별 지원자 결과 저장
     */
    private void saveApplicantResult(FastApiClient.ApplicantResult result) {  // ← 변경
        try {
            // 1. 지원자 정보 업데이트
            Optional<Applicant> applicantOpt = applicantRepository.findById(result.getApplicantId());
            if (applicantOpt.isPresent()) {
                Applicant applicant = applicantOpt.get();

                // 기본 정보 업데이트
                applicant.setInterviewStatus(InterviewStatus.COMPLETED);
                applicant.setCompletedAt(LocalDateTime.now());
                applicant.setIndividualPdfPath(result.getPdfS3Path());
                applicant.setIndividualQnaPath(result.getQnaS3Path());

                // 평가 정보 추출
                Map<String, Object> evaluationJson = result.getEvaluationJson();
                if (evaluationJson != null && !evaluationJson.containsKey("error")) {
                    // 총점 저장
                    Object evaluationSummary = evaluationJson.get("evaluation_summary");
                    if (evaluationSummary instanceof Map) {
                        Map<String, Object> summary = (Map<String, Object>) evaluationSummary;
                        Object totalScore = summary.get("total_score");
                        if (totalScore instanceof Number) {
                            applicant.setTotalScore(((Number) totalScore).floatValue());
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
                }

                applicantRepository.save(applicant);
                log.info("✅ 지원자 정보 저장: {}", result.getApplicantId());

                // 2. 키워드별 점수 저장
                saveKeywordScores(result);
            }

        } catch (Exception e) {
            log.error("❌ 지원자 결과 저장 실패: applicantId={}, error={}",
                    result.getApplicantId(), e.getMessage(), e);
        }
    }

    /**
     * 키워드별 점수 저장
     */
    private void saveKeywordScores(FastApiClient.ApplicantResult result) {  // ← 변경
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

            for (Map.Entry<String, Object> entry : detailed.entrySet()) {
                String keywordName = entry.getKey();
                Object keywordData = entry.getValue();

                if (!(keywordData instanceof Map)) {
                    continue;
                }

                Map<String, Object> keywordEval = (Map<String, Object>) keywordData;

                // 키워드 엔티티 조회
                Optional<Keyword> keywordOpt = keywordRepository.findByKeywordName(keywordName);
                if (keywordOpt.isEmpty()) {
                    log.warn("⚠️ 키워드를 찾을 수 없습니다: {}", keywordName);
                    continue;
                }

                // 점수와 평가 내용 추출
                Object finalScore = keywordEval.get("final_score");
                Object scoreRationale = keywordEval.get("score_rationale");

                if (finalScore instanceof Number) {
                    ApplicantKeywordScore score = new ApplicantKeywordScore();
                    score.setApplicantId(result.getApplicantId());
                    score.setKeywordId(keywordOpt.get().getKeywordId());
                    score.setApplicantScore(((Number) finalScore).intValue());
                    score.setScoreComment(scoreRationale != null ? scoreRationale.toString() : "");

                    applicantKeywordScoreRepository.save(score);
                    log.debug("✅ 키워드 점수 저장: {} - {}점", keywordName, score.getApplicantScore());
                }
            }

        } catch (Exception e) {
            log.error("❌ 키워드 점수 저장 실패: applicantId={}, error={}",
                    result.getApplicantId(), e.getMessage(), e);
        }
    }
}