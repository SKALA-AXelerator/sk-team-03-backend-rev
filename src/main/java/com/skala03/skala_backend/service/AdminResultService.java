// AdminResultService.java
package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.AdminResultDto;
import com.skala03.skala_backend.dto.AdminResultDto.KeywordScoreDto;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.ApplicantKeywordScore;
import com.skala03.skala_backend.entity.Keyword;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.repository.AdminResultRepository;
import com.skala03.skala_backend.repository.ApplicantKeywordScoreRepository;
import com.skala03.skala_backend.repository.KeywordRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
    private final KeywordRepository keywordRepository; // 추가

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
                dto.setInterviewer(session.getInterviewersUserId());
                dto.setInterviewDate(session.getSessionDate().toLocalDate().toString());
                dto.setSessionTime(session.getSessionTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                dto.setSessionLocation(session.getSessionLocation());
            }
        }
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
                        score.getApplicantScore()
                ))
                .collect(Collectors.toList());

        dto.setApplicantKeywordScores(keywordScoreDtos);
    }
}
