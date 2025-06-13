package com.skala03.skala_backend.service;

import com.skala03.skala_backend.dto.InterviewSessionDto;
import com.skala03.skala_backend.entity.RoomParticipant;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.repository.RoomParticipantRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import com.skala03.skala_backend.dto.InterviewSessionDto.ParticipantStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewSessionService {

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private SessionRepository sessionRepository;

    /**
     * 세션 리스트 화면 입장 (offline → waiting)
     */
    public void enterSessionList(String roomId, String userId) {
        Optional<RoomParticipant> participantOpt =
                roomParticipantRepository.findByRoomIdAndUserId(roomId, userId);

        if (participantOpt.isPresent()) {
            RoomParticipant participant = participantOpt.get();
            participant.updateStatus(RoomParticipant.ParticipantStatus.WAITING);
            roomParticipantRepository.save(participant);
        } else {
            throw new IllegalArgumentException("해당 방의 참가자를 찾을 수 없습니다.");
        }
    }

    /**
     * 면접 시작 (방장이 버튼 클릭)
     */
    public boolean startInterview(String roomId, Integer sessionId, String leaderUserId) {
        // 1. 방장 권한 확인
        RoomParticipant leader = roomParticipantRepository
                .findByRoomIdAndUserId(roomId, leaderUserId)
                .orElseThrow(() -> new IllegalArgumentException("방장을 찾을 수 없습니다."));

        if (leader.getParticipantRole() != RoomParticipant.ParticipantRole.LEADER) {
            throw new IllegalStateException("방장만 면접을 시작할 수 있습니다.");
        }

        // 2. 세션 정보 조회
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 3. 해당 세션의 면접관들만 확인
        List<String> interviewerIds = parseUserIds(session.getInterviewersUserId());
        List<RoomParticipant> sessionInterviewers = interviewerIds.stream()
                .map(userId -> roomParticipantRepository.findByRoomIdAndUserId(roomId, userId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // 4. 모든 면접관이 WAITING 상태인지 확인
        boolean allWaiting = sessionInterviewers.stream()
                .allMatch(p -> p.getParticipantStatus() == RoomParticipant.ParticipantStatus.WAITING);

        if (!allWaiting) {
            return false; // 조건 불만족
        }

        // 5. 모든 면접관 상태를 IN_PROGRESS로 변경
        sessionInterviewers.forEach(p ->
                p.updateStatus(RoomParticipant.ParticipantStatus.IN_PROGRESS)
        );
        roomParticipantRepository.saveAll(sessionInterviewers);

        // 6. 세션 상태 변경
        session.setSessionStatus(Session.SessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        return true; // 성공
    }

    /**
     * 참가자 상태 조회
     */
    public InterviewSessionDto.ParticipantStatusResponse getParticipantStatus(String roomId, String userId) {
        Optional<RoomParticipant> participant =
                roomParticipantRepository.findByRoomIdAndUserId(roomId, userId);

        if (participant.isPresent()) {
            RoomParticipant p = participant.get();
            return new InterviewSessionDto.ParticipantStatusResponse(
                    p.getParticipantStatus(),
                    p.getLastPingAt(),
                    "상태 조회 성공"
            );
        }

        throw new IllegalArgumentException("참가자를 찾을 수 없습니다.");
    }

    /**
     * 면접 종료
     */
    public void endInterview(String roomId, Integer sessionId) {
        // 세션 상태 변경
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        session.setSessionStatus(Session.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        // 참가자들 상태를 WAITING으로 변경
        List<String> interviewerIds = parseUserIds(session.getInterviewersUserId());
        interviewerIds.forEach(userId -> {
            roomParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                    .ifPresent(participant -> {
                        participant.updateStatus(RoomParticipant.ParticipantStatus.WAITING);
                        roomParticipantRepository.save(participant);
                    });
        });
    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * "sk-01,sk-02,sk-03" 형태의 문자열을 List로 파싱
     */
    private List<String> parseUserIds(String userIdsString) {
        if (userIdsString == null || userIdsString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(userIdsString.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 모든 면접관이 WAITING 상태인지 확인
     */
    private boolean checkIfAllInterviewersWaiting(String roomId, List<String> interviewerIds) {
        return interviewerIds.stream()
                .allMatch(userId -> {
                    Optional<RoomParticipant> participant =
                            roomParticipantRepository.findByRoomIdAndUserId(roomId, userId);
                    return participant.isPresent() &&
                            participant.get().getParticipantStatus() == RoomParticipant.ParticipantStatus.WAITING;
                });
    }

    public void endSession(String roomId, String userId) {
        Optional<RoomParticipant> participantOpt =
                roomParticipantRepository.findByRoomIdAndUserId(roomId, userId);

        if (participantOpt.isPresent()) {
            RoomParticipant participant = participantOpt.get();
            participant.updateStatus(RoomParticipant.ParticipantStatus.OFFLINE);
            roomParticipantRepository.save(participant);
        } else {
            throw new IllegalArgumentException("해당 방의 참가자를 찾을 수 없습니다.");
        }
    }
}