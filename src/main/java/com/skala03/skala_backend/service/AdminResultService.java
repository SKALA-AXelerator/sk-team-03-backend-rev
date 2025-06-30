// AdminResultService.java
package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.AdminResultDto;
import com.skala03.skala_backend.dto.AdminResultDto.KeywordScoreDto;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.ApplicantKeywordScore;
import com.skala03.skala_backend.entity.Keyword;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.entity.User;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminResultService {

    private final AdminResultRepository adminResultRepository;
    private final SessionRepository sessionRepository;
    private final ApplicantKeywordScoreRepository keywordScoreRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository; // ✅ UserRepository 추가

    public List<AdminResultDto> getJobRoleResults(String jobRoleId) {
        List<Applicant> applicants = adminResultRepository.findApplicantsByJobRoleId(jobRoleId);

        return applicants.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private AdminResultDto convertToDto(Applicant applicant) {
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

        // 키워드 점수 매핑
        setKeywordScores(dto, applicant);

        return dto;
    }

    private void setSessionInfo(AdminResultDto dto, Applicant applicant) {
        if (applicant.getSessionId() != null) {
            Optional<Session> sessionOpt = sessionRepository.findById(applicant.getSessionId());
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();

                // ✅ 면접관 아이디들을 이름으로 변환
                String interviewerNames = convertInterviewerIdsToNames(session.getInterviewersUserId());
                dto.setInterviewer(interviewerNames);

                dto.setInterviewDate(session.getSessionDate().toLocalDate().toString());
                dto.setSessionTime(session.getSessionTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                dto.setSessionLocation(session.getSessionLocation());
            }
        }
    }

    /**
     * ✅ 면접관 아이디 문자열을 면접관 이름 문자열로 변환
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

        // 각 아이디로 사용자 이름 조회 (성능 최적화를 위해 배치 조회도 가능)
        List<String> nameList = idList.stream()
                .map(id -> userRepository.findById(id)
                        .map(User::getUserName)
                        .orElse("Unknown")) // 사용자를 찾을 수 없는 경우 "Unknown"
                .collect(Collectors.toList());

        return String.join(", ", nameList);
    }

    private void setKeywordScores(AdminResultDto dto, Applicant applicant) {
        List<ApplicantKeywordScore> keywordScores =
                keywordScoreRepository.findByApplicantId(applicant.getApplicantId());

        // 키워드 ID 목록 추출
        List<Integer> keywordIds = keywordScores.stream()
                .map(ApplicantKeywordScore::getKeywordId)
                .collect(Collectors.toList());

        // 키워드 정보 배치 조회 (성능 최적화)
        Map<Integer, String> keywordMap = keywordRepository.findAllById(keywordIds)
                .stream()
                .collect(Collectors.toMap(Keyword::getKeywordId, Keyword::getKeywordName));

        List<KeywordScoreDto> keywordScoreDtos = keywordScores.stream()
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