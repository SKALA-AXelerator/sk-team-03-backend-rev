package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.InterviewerResponse;
import com.skala03.skala_backend.dto.InterviewScheduleRequest;
import com.skala03.skala_backend.dto.InterviewScheduleResponse;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.entity.User;
import com.skala03.skala_backend.repository.ApplicantRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import com.skala03.skala_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewerService {

    private final SessionRepository sessionRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;

    // 기존 기능 - 통계용
    public InterviewerResponse getRoomsForInterviewer(String userId) {
        List<Session> sessions = sessionRepository.findByUserIdInInterviewers(userId);

        List<InterviewerResponse.RoomInfo> roomList = new ArrayList<>();

        for (Session session : sessions) {
            // 1. 인터뷰어 리스트 가져오기
            String[] interviewerIds = session.getInterviewersUserId().split(",");
            List<InterviewerResponse.Interviewer> interviewers = Arrays.stream(interviewerIds)
                    .map(id -> {
                        User user = userRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("User not found: " + id));
                        return new InterviewerResponse.Interviewer(user.getUserId(), user.getUserName());
                    })
                    .collect(Collectors.toList());

            // 2. 지원자 총 수
            int totalCount = applicantRepository.countBySessionId(session.getSessionId());
            // 3. 대기 중인 지원자 수
            int remainingCount = applicantRepository.countWaitingBySessionId(session.getSessionId());

            InterviewerResponse.RoomInfo roomInfo = new InterviewerResponse.RoomInfo(
                    interviewers,
                    totalCount,
                    remainingCount
            );

            roomList.add(roomInfo);
        }

        return new InterviewerResponse(roomList);
    }

    public List<InterviewScheduleResponse> getInterviewSchedules(List<String> interviewerIds) {
        List<Session> sessions = sessionRepository.findAll().stream()
                .filter(s -> {
                    if (s.getInterviewersUserId() == null) return false;
                    Set<String> sessionInterviewerSet = new HashSet<>(Arrays.asList(s.getInterviewersUserId().split(",")));
                    return sessionInterviewerSet.containsAll(interviewerIds);  // ✅ 모든 ID가 포함되어야 통과
                })
                .collect(Collectors.toList());

        return sessions.stream().map(session -> {
            List<Applicant> applicants = applicantRepository.findBySessionId(session.getSessionId());

            List<InterviewScheduleResponse.ApplicantDto> applicantDtos = applicants.stream()
                    .map(a -> new InterviewScheduleResponse.ApplicantDto(a.getApplicantId(), a.getApplicantName()))
                    .collect(Collectors.toList());

            return new InterviewScheduleResponse(
                    session.getRoomId(),
                    session.getSessionTime().toString(),
                    applicantDtos
            );
        }).collect(Collectors.toList());
    }
}