package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.InterviewerResponse;
import com.skala03.skala_backend.dto.InterviewScheduleResponse;
import com.skala03.skala_backend.entity.Applicant;
import com.skala03.skala_backend.entity.InterviewRoom;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.entity.User;
import com.skala03.skala_backend.repository.ApplicantRepository;
import com.skala03.skala_backend.repository.InterviewRoomRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import com.skala03.skala_backend.repository.UserRepository;
import com.skala03.skala_backend.entity.RoomParticipant;
import com.skala03.skala_backend.repository.RoomParticipantRepository;
import org.springframework.transaction.annotation.Transactional;
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
    private final InterviewRoomRepository interviewRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;

    /**
     * 면접관이 참여하는 룸별 정보 조회 (간소화 버전)
     */
    public InterviewerResponse getRoomsForInterviewer(String userId) {

        List<Session> sessions = sessionRepository.findByUserIdInInterviewers(userId);

        if (sessions.isEmpty()) {
            return new InterviewerResponse(new ArrayList<>());
        }

        Map<String, List<Session>> sessionsByRoom = sessions.stream()
                .collect(Collectors.groupingBy(Session::getRoomId));

        List<InterviewerResponse.RoomInfo> roomList = new ArrayList<>();

        for (Map.Entry<String, List<Session>> entry : sessionsByRoom.entrySet()) {
            String roomId = entry.getKey();
            List<Session> roomSessions = entry.getValue();

            try {
                InterviewRoom room = interviewRoomRepository.findById(roomId)
                        .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

                // 첫 번째 세션의 면접관 정보 사용 (모든 세션의 면접관이 동일)
                List<InterviewerResponse.Interviewer> interviewers = new ArrayList<>();

                if (!roomSessions.isEmpty() && roomSessions.get(0).getInterviewersUserId() != null) {
                    String[] interviewerIds = roomSessions.get(0).getInterviewersUserId().split(",");

                    interviewers = Arrays.stream(interviewerIds)
                            .map(String::trim)
                            .filter(id -> !id.isEmpty())
                            .map(id -> {
                                User user = userRepository.findById(id).orElse(null);
                                if (user != null) {
                                    return new InterviewerResponse.Interviewer(user.getUserId(), user.getUserName());
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }

                // ✅ 기존 메서드 사용하여 통계 계산
                int totalCount = 0;
                int remainingCount = 0;

                for (Session session : roomSessions) {
                    // sessionId를 int로 변환 (기존 메서드가 int 파라미터 사용)
                    int sessionIdInt = session.getSessionId().intValue();

                    totalCount += applicantRepository.countBySessionId(sessionIdInt);
                    remainingCount += applicantRepository.countWaitingBySessionId(sessionIdInt);
                }

                InterviewerResponse.RoomInfo roomInfo = new InterviewerResponse.RoomInfo(
                        roomId,
                        room.getLeaderUserId(),
                        interviewers,
                        totalCount,
                        remainingCount
                );

                roomList.add(roomInfo);

            } catch (Exception e) {
                // 로그 처리
            }
        }

        // 최종 결과만 정렬 (한 줄 추가)
        roomList.sort(Comparator.comparing(InterviewerResponse.RoomInfo::getRoomId));
        return new InterviewerResponse(roomList);
    }

    /**
     * ✅ 룸 ID로 해당 룸의 모든 세션 조회 (sessionId + sessionStatus 포함)
     */
    public List<InterviewScheduleResponse> getRoomSessions(String roomId) {

        // 1. 룸 정보 조회
        InterviewRoom room = interviewRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        // 2. 해당 룸의 모든 세션 조회
        List<Session> sessions = sessionRepository.findByRoomId(roomId);

        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 각 세션을 InterviewScheduleResponse로 변환
        List<InterviewScheduleResponse> responses = sessions.stream()
                .map(session -> {
                    try {
                        // 지원자 목록 조회
                        int sessionIdInt = session.getSessionId().intValue();
                        List<Applicant> applicants = applicantRepository.findBySessionId(sessionIdInt);

                        // 지원자 DTO 변환
                        List<InterviewScheduleResponse.ApplicantDto> applicantList = applicants.stream()
                                .map(a -> new InterviewScheduleResponse.ApplicantDto(
                                        a.getApplicantId(),   // id
                                        a.getApplicantName()  // name
                                ))
                                .collect(Collectors.toList());

                        // 면접 시간 포맷팅
                        String interviewTime = session.getSessionTime().toString();
                        // 또는 원하는 포맷으로:
                        // String interviewTime = session.getSessionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                        // ✅ sessionId + sessionStatus 포함한 InterviewScheduleResponse 생성
                        return new InterviewScheduleResponse(
                                session.getSessionId(),      // sessionId
                                room.getRoomName(),          // interviewRoom
                                interviewTime,               // interviewTime
                                session.getSessionStatus(),  // ✅ sessionStatus 추가
                                applicantList                // applicantList
                        );

                    } catch (Exception e) {

                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InterviewScheduleResponse::getInterviewTime))  // 시간순 정렬
                .collect(Collectors.toList());


        return responses;
    }

    /**
     * ✅ 면접관 상태 변경 (단순 버전) - 이 메서드를 맨 마지막에 추가하세요
     */
    @Transactional
    public String updateInterviewerStatus(String userId, String statusStr) {

        // 1. 유효한 상태값인지 확인
        RoomParticipant.ParticipantStatus status;
        try {
            status = RoomParticipant.ParticipantStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다. (OFFLINE, WAITING, IN_PROGRESS만 가능)");
        }

        // 2. 해당 사용자의 모든 룸 참가 정보 조회 후 상태 변경
        List<RoomParticipant> participants = roomParticipantRepository.findByUserId(userId);

        if (participants.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자의 참가 정보를 찾을 수 없습니다.");
        }

        // 3. 모든 룸에서의 상태 업데이트
        for (RoomParticipant participant : participants) {
            participant.updateStatus(status);
        }
        roomParticipantRepository.saveAll(participants);

        return "면접관 상태가 " + status.name() + "로 변경되었습니다.";
    }
}
