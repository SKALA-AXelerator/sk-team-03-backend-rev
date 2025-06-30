package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.AdminResultDto;
import com.skala03.skala_backend.dto.AdminResultDto.KeywordScoreDto;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.ApplicantKeywordScore;
import com.skala03.skala_backend.entity.JobRoleKeyword; // ✅ 추가
import com.skala03.skala_backend.entity.Keyword;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.entity.User;
import com.skala03.skala_backend.repository.AdminRepository; // ✅ 추가
import com.skala03.skala_backend.repository.AdminResultRepository;
import com.skala03.skala_backend.repository.ApplicantKeywordScoreRepository;
import com.skala03.skala_backend.repository.KeywordRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import com.skala03.skala_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminResultService {

    private final AdminResultRepository adminResultRepository;
    private final SessionRepository sessionRepository;
    private final ApplicantKeywordScoreRepository keywordScoreRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository; // ✅ AdminRepository 추가

    public List<AdminResultDto> getJobRoleResults(String jobRoleId) {
        List<Applicant> applicants = adminResultRepository.findApplicantsByJobRoleId(jobRoleId);

        return applicants.stream()
                .map(applicant -> convertToDto(applicant, jobRoleId)) // ✅ jobRoleId 전달
                .collect(Collectors.toList());
    }

    // ✅ jobRoleId 파라미터 추가
    private AdminResultDto convertToDto(Applicant applicant, String jobRoleId) {
        AdminResultDto dto = new AdminResultDto();

        // 기본 지원자 정보
        dto.setApplicantId(applicant.getApplicantId());
        dto.setApplicantName(applicant.getApplicantName());
        dto.setIndividualPdfPath(applicant.getIndividualPdfPath());
        dto.setIndividualQnaPath(applicant.getIndividualQnaPath());
        dto.setJobRoleName(applicant.getJobRole().getJobRoleName());
        dto.setInterviewStatus(applicant.getInterviewStatus().toString());
        dto.setTotalScore(applicant.getTotalScore());

        // 세션 정보 매핑
        setSessionInfo(dto, applicant);

        // ✅ 선택된 키워드만 필터링하여 키워드 점수 매핑
        setSelectedKeywordScores(dto, applicant, jobRoleId);

        return dto;
    }

    private void setSessionInfo(AdminResultDto dto, Applicant applicant) {
        if (applicant.getSessionId() != null) {
            Optional<Session> sessionOpt = sessionRepository.findById(applicant.getSessionId());
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();

                // 면접관 아이디들을 이름으로 변환
                String interviewerNames = convertInterviewerIdsToNames(session.getInterviewersUserId());
                dto.setInterviewer(interviewerNames);

                dto.setInterviewDate(session.getSessionDate().toLocalDate().toString());
                dto.setSessionTime(session.getSessionTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                dto.setSessionLocation(session.getSessionLocation());
            }
        }
    }

    /**
     * 면접관 아이디 문자열을 면접관 이름 문자열로 변환
     * 예: "sk-01,sk-02,sk-03" → "김철수, 박영희, 이민수"
     */
    private String convertInterviewerIdsToNames(String interviewerIds) {
        if (interviewerIds == null || interviewerIds.trim().isEmpty()) {
            return "";
        }

        // 쉼표로 구분된 아이디들을 파싱
        List<String> idList = Arrays.stream(interviewerIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        // 각 아이디로 사용자 이름 조회
        List<String> nameList = idList.stream()
                .map(id -> userRepository.findById(id)
                        .map(User::getUserName)
                        .orElse("Unknown")) // 사용자를 찾을 수 없는 경우 "Unknown"
                .collect(Collectors.toList());

        return String.join(", ", nameList);
    }

    /**
     * ✅ 선택된 키워드만 필터링하여 키워드 점수 설정
     */
    private void setSelectedKeywordScores(AdminResultDto dto, Applicant applicant, String jobRoleId) {
        // 1. 해당 직무에서 선택된 키워드 ID 목록 조회
        List<JobRoleKeyword> jobRoleKeywords = adminRepository.findJobRoleKeywordsByJobRoleId(jobRoleId);

        Set<Integer> selectedKeywordIds = jobRoleKeywords.stream()
                .filter(jrk -> jrk.getSelected() != null && jrk.getSelected()) // selected가 true인 것만
                .map(JobRoleKeyword::getKeywordId)
                .collect(Collectors.toSet());

        // 선택된 키워드가 없는 경우 빈 리스트 반환
        if (selectedKeywordIds.isEmpty()) {
            dto.setApplicantKeywordScores(List.of());
            return;
        }

        // 2. 지원자의 모든 키워드 점수 조회
        List<ApplicantKeywordScore> allKeywordScores =
                keywordScoreRepository.findByApplicantId(applicant.getApplicantId());

        // 3. 선택된 키워드에 해당하는 점수만 필터링
        List<ApplicantKeywordScore> selectedKeywordScores = allKeywordScores.stream()
                .filter(score -> selectedKeywordIds.contains(score.getKeywordId()))
                .collect(Collectors.toList());

        // 4. 키워드 정보 배치 조회 (성능 최적화)
        List<Integer> keywordIds = selectedKeywordScores.stream()
                .map(ApplicantKeywordScore::getKeywordId)
                .collect(Collectors.toList());

        if (keywordIds.isEmpty()) {
            dto.setApplicantKeywordScores(List.of());
            return;
        }

        Map<Integer, String> keywordMap = keywordRepository.findAllById(keywordIds)
                .stream()
                .collect(Collectors.toMap(Keyword::getKeywordId, Keyword::getKeywordName));

        // 5. DTO 변환
        List<KeywordScoreDto> keywordScoreDtos = selectedKeywordScores.stream()
                .map(score -> new KeywordScoreDto(
                        score.getKeywordId().toString(),
                        keywordMap.getOrDefault(score.getKeywordId(), "Unknown"),
                        score.getApplicantScore(),
                        score.getScoreComment()
                ))
                .collect(Collectors.toList());

        dto.setApplicantKeywordScores(keywordScoreDtos);
    }

}