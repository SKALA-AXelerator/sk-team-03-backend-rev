package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.ApplicantDto;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.InterviewStatus;
import com.skala03.skala_backend.entity.JobRole;
import com.skala03.skala_backend.entity.Session;  // ✅ 추가
import com.skala03.skala_backend.repository.ApplicantRepository;
import com.skala03.skala_backend.repository.SessionRepository;  // ✅ 추가
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicantService {

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private SessionRepository sessionRepository;  // ✅ 추가

    // 전체 지원자 리스트 조회
    @Transactional(readOnly = true)
    public ApplicantDto.ListResponse getAllApplicants() {
        List<Applicant> applicants = applicantRepository.findAll();
        List<ApplicantDto.ApplicantInfo> applicantInfos = applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(
                        a.getApplicantId(),
                        a.getApplicantName(),
                        a.getInterviewStatus() != null ? a.getInterviewStatus().toString() : "UNKNOWN"  // 추가
                ))
                .collect(Collectors.toList());
        return new ApplicantDto.ListResponse(applicantInfos);
    }

    // ===== Service 메서드 수정 =====
    @Transactional(readOnly = true)
    public ApplicantDto.QuestionsResponse getApplicantQuestions(ApplicantDto.QuestionsRequest request) {
        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(request.getApplicantIds());

        if (applicants.isEmpty()) {
            throw new IllegalArgumentException("지원자 정보를 찾을 수 없습니다.");
        }

        // 첫 번째 지원자의 직무 정보 조회 (모든 지원자가 같은 직무이므로)
        JobRole jobRole = applicants.get(0).getJobRole();
        ApplicantDto.JobRoleInfo jobRoleInfo = new ApplicantDto.JobRoleInfo(
                jobRole.getJobRoleName()  // 🔧 jobRoleName만 사용
        );

        // 지원자별 질문 정보 조회
        List<ApplicantDto.QuestionInfo> questionList = applicants.stream()
                .map(applicant -> {
                    // 실제 DB에서 지원자별 질문 조회
                    List<String> questions = applicantRepository.findQuestionsByApplicantId(applicant.getApplicantId());

                    return new ApplicantDto.QuestionInfo(
                            applicant.getApplicantId(),
                            applicant.getApplicantName(),
                            questions
                    );
                })
                .collect(Collectors.toList());

        return new ApplicantDto.QuestionsResponse(jobRoleInfo, questionList);
    }

    // 지원자 평가 (AI 분석) - DB 데이터 사용 버전 (직무 정보 포함)
    public List<ApplicantDto.EvaluationResponse> evaluateApplicants(ApplicantDto.EvaluationRequest request) {
        List<String> applicantIds = request.getApplicantIds();

        // JobRole을 함께 fetch하여 N+1 문제 방지
        List<Applicant> applicants = applicantRepository.findByApplicantIdInWithJobRole(applicantIds);

        return applicants.stream()
                .map(applicant -> {
                    // 실제 DB에서 키워드 점수 조회
                    List<Object[]> keywordScores = applicantRepository.findKeywordScoresByApplicantId(applicant.getApplicantId());

                    List<ApplicantDto.KeywordEvaluation> evaluations = keywordScores.stream()
                            .map(row -> new ApplicantDto.KeywordEvaluation(
                                    (String) row[0],    // keyword_name
                                    ((Number) row[1]).intValue(),  // applicant_score
                                    (String) row[2]     // score_comment
                            ))
                            .collect(Collectors.toList());

                    // TODO: S3 presigned URL 생성 로직 구현 필요
                    String summaryUrl = "https://example-s3-bucket.com/summaries/" + applicant.getApplicantId() + ".txt";

                    // 직무명 가져오기
                    String jobRoleName = applicant.getJobRole() != null ? applicant.getJobRole().getJobRoleName() : null;

                    // 평가 점수 업데이트 (실제 점수 평균으로)
                    if (!evaluations.isEmpty()) {
                        Float totalScore = (float) evaluations.stream()
                                .mapToInt(ApplicantDto.KeywordEvaluation::getScore)
                                .average()
                                .orElse(0.0);
                        applicant.setTotalScore(totalScore);
                    }

                    // 직무 정보를 evaluations 리스트 맨 앞에 추가
                    if (applicant.getJobRole() != null) {
                        ApplicantDto.KeywordEvaluation jobRoleInfo = new ApplicantDto.KeywordEvaluation(
                                "직무정보: " + applicant.getJobRole().getJobRoleName(),
                                0,  // 점수는 0으로 설정 (직무정보이므로)
                                "지원자의 직무 정보"
                        );
                        evaluations.add(0, jobRoleInfo);  // 맨 앞에 추가
                    }

                    return new ApplicantDto.EvaluationResponse(
                            applicant.getApplicantId(),
                            applicant.getApplicantName(),
                            jobRoleName,  // 직무명 추가
                            evaluations,
                            summaryUrl
                    );
                })
                .collect(Collectors.toList());
    }

    // 면접 종료 후 모든 면접자 완료 상태로 변경
    public void updateToInterviewComplete(ApplicantDto.StatusUpdateRequest request) {
        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(request.getApplicationIds());

        applicants.forEach(applicant -> {
            applicant.setInterviewStatus(InterviewStatus.COMPLETED);
            applicant.setCompletedAt(LocalDateTime.now());
        });

        applicantRepository.saveAll(applicants);
    }

    // 지원자 수동 추가 후 DB 처리 메서드
    public void directAddApplicant(ApplicantDto.DetailedStatusUpdateRequest request) {
        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(request.getApplicantIds());

        // 요청으로 받은 상태 문자열을 InterviewStatus enum으로 변환
        InterviewStatus newStatus = null;
        if (request.getInterviewStatus() != null) {
            try {
                newStatus = InterviewStatus.valueOf(request.getInterviewStatus().toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid interview status: " + request.getInterviewStatus() +
                        ". Valid values are: pending, completed, absent");
            }
        }

        final InterviewStatus finalNewStatus = newStatus;

        applicants.forEach(applicant -> {
            // TODO: 면접관 정보와 방 정보 업데이트 로직 구현
            // room_participants 테이블과 연동 필요

            // 면접 상태 업데이트
            if (finalNewStatus != null) {
                applicant.setInterviewStatus(finalNewStatus);

                // 상태별 시간 기록
                switch (finalNewStatus) {
                    case WAITING:
                        // 대기 상태로 변경 시 시작/완료 시간 초기화 (필요시)
                        // applicant.setStartedAt(null);
                        // applicant.setCompletedAt(null);
                        break;
                    case COMPLETED:
                        // 완료 상태로 변경 시 완료 시간 기록
                        applicant.setCompletedAt(LocalDateTime.now());
                        if (applicant.getStartedAt() == null) {
                            applicant.setStartedAt(LocalDateTime.now());
                        }
                        break;
                    case ABSENT:
                        // 불참 처리 - 특별한 시간 기록 없음
                        break;
                }
            }

            // 면접 시작 시간 기록 (상태 변경과 별개로)
            if (applicant.getStartedAt() == null && finalNewStatus != InterviewStatus.WAITING) {
                applicant.setStartedAt(LocalDateTime.now());
            }
        });

        applicantRepository.saveAll(applicants);
    }

    // 세션별 지원자 조회 (수정된 메서드)
    @Transactional(readOnly = true)
    public List<ApplicantDto.ApplicantInfo> getApplicantsBySession(Integer sessionId) {
        List<Applicant> applicants = applicantRepository.findBySessionId(sessionId);
        return applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(
                        a.getApplicantId(),  // id 필드에 매핑
                        a.getApplicantName(), // name 필드에 매핑
                        a.getInterviewStatus() != null ? a.getInterviewStatus().toString() : "UNKNOWN"  // interviewStatus 필드에 매핑
                ))
                .collect(Collectors.toList());
    }

    // 직무별 지원자 조회 (수정된 메서드)
    @Transactional(readOnly = true)
    public List<ApplicantDto.ApplicantInfo> getApplicantsByJobRole(String jobRoleId) {
        List<Applicant> applicants = applicantRepository.findByJobRoleId(jobRoleId);
        return applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(
                        a.getApplicantId(),
                        a.getApplicantName(),
                        a.getInterviewStatus() != null ? a.getInterviewStatus().toString() : "UNKNOWN"  // 추가
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicantDto.StatusChangeResponse updateApplicantStatus(String applicantId, ApplicantDto.StatusChangeRequest request) {
        // 입력값 검증
        if (request.getInterviewStatus() == null || request.getInterviewStatus().trim().isEmpty()) {
            throw new RuntimeException("Interview status cannot be null or empty");
        }

        // 지원자 존재 여부 확인
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found: " + applicantId));

        // 이전 상태 저장
        String previousStatus = applicant.getInterviewStatus().name();

        // 요청으로 받은 상태 문자열을 InterviewStatus enum으로 변환
        InterviewStatus newStatus;
        try {
            // toLowerCase() → toUpperCase()로 변경
            newStatus = InterviewStatus.valueOf(request.getInterviewStatus().toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid interview status: " + request.getInterviewStatus() +
                    ". Valid values are: WAITING, COMPLETED, ABSENT");
        }

        // 상태 업데이트
        applicant.setInterviewStatus(newStatus);

        // 상태별 시간 기록
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case WAITING:
                // 대기 상태로 변경 시 시작/완료 시간 초기화 (선택사항)
                applicant.setStartedAt(null);
                applicant.setCompletedAt(null);
                break;
            case COMPLETED:
                // 완료 상태로 변경 시 완료 시간 기록
                applicant.setCompletedAt(now);
                if (applicant.getStartedAt() == null) {
                    applicant.setStartedAt(now);
                }
                break;
            case ABSENT:
                // 불참 처리 - 필요시 특별한 로직 추가
                break;
        }

        // 저장
        applicantRepository.save(applicant);

        // 응답 생성
        String message = "Interview status updated successfully from " + previousStatus + " to " + newStatus.name();

        return new ApplicantDto.StatusChangeResponse(
                applicantId,
                newStatus.name(),
                message
        );
    }

    // ✅ 세션 재편성 (기존 세션 정보 완전히 복사 + 빈 세션 자동 삭제)
    @Transactional
    public ApplicantDto.SessionReorganizeResponse reorganizeSessions(
            ApplicantDto.SessionReorganizeRequest request) {

        List<String> selectedApplicantIds = request.getSelectedApplicantIds();
        String roomId = request.getRoomId();

        // 기본 검증
        if (roomId == null || roomId.isEmpty()) {
            throw new RuntimeException("Room ID is required");
        }

        // 1. 선택된 지원자들의 기존 세션 정보 조회
        List<Applicant> selectedApplicants = applicantRepository.findByApplicantIdIn(selectedApplicantIds);
        if (selectedApplicants.isEmpty()) {
            throw new RuntimeException("Selected applicants not found");
        }

        Map<Integer, List<Applicant>> sessionGroups = selectedApplicants.stream()
                .collect(Collectors.groupingBy(Applicant::getSessionId));

        // 2. ✅ 기존 세션 정보 조회 (첫 번째 지원자의 세션 정보 사용)
        Integer originalSessionId = selectedApplicants.get(0).getSessionId();
        Session originalSession = sessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new RuntimeException("Original session not found: " + originalSessionId));

        // 3. 새 세션 ID 생성
        Integer newSessionId = applicantRepository.findMaxSessionIdFromSessions() + 1;

        // 4. ✅ 기존 세션 정보를 완전히 복사하여 새 세션 생성 (session_id와 applicants_user_id만 다름)
        String applicantIdsStr = String.join(",", selectedApplicantIds);

        applicantRepository.createNewSessionFromExisting(
                newSessionId,
                applicantIdsStr,
                originalSessionId
        );

        // 5. 선택된 지원자들을 새 세션으로 이동
        selectedApplicants.forEach(applicant -> {
            applicant.setSessionId(newSessionId);
            applicant.setInterviewStatus(InterviewStatus.WAITING);
            applicant.setStartedAt(null);
            applicant.setCompletedAt(null);
        });
        applicantRepository.saveAll(selectedApplicants);

        List<ApplicantDto.SessionUpdateInfo> updatedSessions = new ArrayList<>();
        List<Integer> deletedSessionIds = new ArrayList<>();

        // 6. 기존 세션들의 applicants_user_id 업데이트
        for (Integer sessionId : sessionGroups.keySet()) {
            // 해당 세션의 남은 지원자들 조회
            List<Applicant> remainingApplicants = applicantRepository
                    .findBySessionIdAndApplicantIdNotIn(sessionId, selectedApplicantIds);

            List<String> remainingApplicantIds = remainingApplicants.stream()
                    .map(Applicant::getApplicantId)
                    .collect(Collectors.toList());

            if (remainingApplicantIds.isEmpty()) {
                // 빈 세션 삭제
                applicantRepository.deleteSessionById(sessionId);
                deletedSessionIds.add(sessionId);

                updatedSessions.add(new ApplicantDto.SessionUpdateInfo(
                        sessionId,
                        remainingApplicantIds,
                        "deleted"
                ));
            } else {
                // sessions 테이블의 applicants_user_id 업데이트
                String updatedApplicantIds = String.join(",", remainingApplicantIds);
                applicantRepository.updateSessionApplicants(sessionId, updatedApplicantIds);

                updatedSessions.add(new ApplicantDto.SessionUpdateInfo(
                        sessionId,
                        remainingApplicantIds,
                        "updated"
                ));
            }
        }

        String message = String.format(
                "Sessions reorganized successfully. New session %d created with %d applicants. %d empty sessions deleted. All session info copied from session %d.",
                newSessionId, selectedApplicantIds.size(), deletedSessionIds.size(), originalSessionId
        );

        return new ApplicantDto.SessionReorganizeResponse(
                newSessionId,
                selectedApplicantIds,
                updatedSessions,
                message
        );
    }
}