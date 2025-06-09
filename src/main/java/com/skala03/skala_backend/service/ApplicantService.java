package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.ApplicantDto;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.InterviewStatus;
import com.skala03.skala_backend.repository.ApplicantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@Transactional
public class ApplicantService {

    @Autowired
    private ApplicantRepository applicantRepository;

    // 전체 지원자 리스트 조회
    @Transactional(readOnly = true)
    public ApplicantDto.ListResponse getAllApplicants() {
        List<Applicant> applicants = applicantRepository.findAll();
        List<ApplicantDto.ApplicantInfo> applicantInfos = applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(a.getApplicantId(), a.getApplicantName()))
                .collect(Collectors.toList());
        return new ApplicantDto.ListResponse(applicantInfos);
    }

    // 지원자별 질문 리스트 조회
    @Transactional(readOnly = true)
    public ApplicantDto.QuestionsResponse getApplicantQuestions(ApplicantDto.QuestionsRequest request) {
        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(request.getApplicantIds());

        List<ApplicantDto.QuestionInfo> questionList = applicants.stream()
                .map(applicant -> {
                    // TODO: 실제 질문 데이터는 Question 서비스에서 가져와야 함
                    List<String> mockQuestions = Arrays.asList(
                            applicant.getApplicantName() + "님, 자기소개를 해주세요.",
                            applicant.getApplicantName() + "님의 강점은 무엇인가요?",
                            applicant.getApplicantName() + "님이 지원한 이유는 무엇인가요?"
                    );
                    return new ApplicantDto.QuestionInfo(
                            applicant.getApplicantId(),
                            applicant.getApplicantName(),
                            mockQuestions
                    );
                })
                .collect(Collectors.toList());

        return new ApplicantDto.QuestionsResponse(questionList);
    }

    // 지원자 평가 (AI 분석)
    public List<ApplicantDto.EvaluationResponse> evaluateApplicants(ApplicantDto.EvaluationRequest request) {
        List<String> applicantIds = request.getApplicants().stream()
                .map(ApplicantDto.BasicInfo::getId)
                .collect(Collectors.toList());

        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(applicantIds);

        return applicants.stream()
                .map(applicant -> {
                    // TODO: 실제 AI 분석 로직 구현 필요
                    List<ApplicantDto.KeywordEvaluation> mockEvaluations = Arrays.asList(
                            new ApplicantDto.KeywordEvaluation("의사소통", 4, "명확하고 논리적인 의사소통 능력을 보여줌"),
                            new ApplicantDto.KeywordEvaluation("문제해결능력", 5, "복잡한 문제에 대한 창의적 해결책 제시"),
                            new ApplicantDto.KeywordEvaluation("팀워크", 3, "협업 경험은 있으나 구체적 사례 부족"),
                            new ApplicantDto.KeywordEvaluation("적극성", 4, "업무에 대한 적극적인 자세를 보임")
                    );

                    // TODO: S3 presigned URL 생성 로직 구현 필요
                    String summaryUrl = "https://example-s3-bucket.com/summaries/" + applicant.getApplicantId() + ".txt";

                    // 평가 점수 업데이트 (선택사항)
                    Float totalScore = (float) mockEvaluations.stream()
                            .mapToInt(ApplicantDto.KeywordEvaluation::getScore)
                            .average()
                            .orElse(0.0);
                    applicant.setTotalScore(totalScore);

                    return new ApplicantDto.EvaluationResponse(
                            applicant.getApplicantId(),
                            applicant.getApplicantName(),
                            mockEvaluations,
                            summaryUrl
                    );
                })
                .collect(Collectors.toList());
    }

    // 면접 완료 상태로 변경
    public void updateToInterviewComplete(ApplicantDto.StatusUpdateRequest request) {
        List<Applicant> applicants = applicantRepository.findByApplicantIdIn(request.getApplicationIds());

        applicants.forEach(applicant -> {
            applicant.setInterviewStatus(InterviewStatus.completed);
            applicant.setCompletedAt(LocalDateTime.now());
        });

        applicantRepository.saveAll(applicants);
    }

    // 지원자 상태 업데이트 (면접관, 방 정보 + 면접 상태 포함)
    public void updateApplicantStatus(ApplicantDto.DetailedStatusUpdateRequest request) {
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
                    case waiting:
                        // 대기 상태로 변경 시 시작/완료 시간 초기화 (필요시)
                        // applicant.setStartedAt(null);
                        // applicant.setCompletedAt(null);
                        break;
                    case completed:
                        // 완료 상태로 변경 시 완료 시간 기록
                        applicant.setCompletedAt(LocalDateTime.now());
                        if (applicant.getStartedAt() == null) {
                            applicant.setStartedAt(LocalDateTime.now());
                        }
                        break;
                    case absent:
                        // 불참 처리 - 특별한 시간 기록 없음
                        break;
                }
            }

            // 면접 시작 시간 기록 (상태 변경과 별개로)
            if (applicant.getStartedAt() == null && finalNewStatus != InterviewStatus.waiting) {
                applicant.setStartedAt(LocalDateTime.now());
            }
        });

        applicantRepository.saveAll(applicants);
    }

    // 세션별 지원자 조회 (추가 메서드)
    @Transactional(readOnly = true)
    public List<ApplicantDto.ApplicantInfo> getApplicantsBySession(Integer sessionId) {
        List<Applicant> applicants = applicantRepository.findBySessionId(sessionId);
        return applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(a.getApplicantId(), a.getApplicantName()))
                .collect(Collectors.toList());
    }

    // 직무별 지원자 조회 (추가 메서드)
    @Transactional(readOnly = true)
    public List<ApplicantDto.ApplicantInfo> getApplicantsByJobRole(String jobRoleId) {
        List<Applicant> applicants = applicantRepository.findByJobRoleId(jobRoleId);
        return applicants.stream()
                .map(a -> new ApplicantDto.ApplicantInfo(a.getApplicantId(), a.getApplicantName()))
                .collect(Collectors.toList());
    }
}