package com.skala03.skala_backend.service;

import com.skala03.skala_backend.entity.InterviewContent;
import com.skala03.skala_backend.entity.RoomParticipant;
import com.skala03.skala_backend.entity.Session;
import com.skala03.skala_backend.repository.InterviewContentRepository;
import com.skala03.skala_backend.repository.RoomParticipantRepository;
import com.skala03.skala_backend.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewSessionService {

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private InterviewContentRepository interviewContentRepository;

    /**
     * 세션 리스트 화면 입장 (offline → waiting)
     */
    public void enterSessionList(String roomId, String userId) {
        RoomParticipant participant = roomParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참가자를 찾을 수 없습니다."));

        participant.updateStatus(RoomParticipant.ParticipantStatus.WAITING);
        roomParticipantRepository.save(participant);
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
        session.setSessionStatus(Session.SessionStatus.WAITING);
        sessionRepository.save(session);

        return true; // 성공
    }
    /**
     * 세션 상태 조회 - sessionStatus만 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionStatus(Integer sessionId) {


        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {

            throw new RuntimeException("세션을 찾을 수 없습니다: " + sessionId);
        }

        Session session = sessionOpt.get();

        Map<String, Object> result = new HashMap<>();
        result.put("sessionStatus", session.getSessionStatus().name());



        return result;
    }
    /**
     * 참가자 상태 조회 - Map으로 직접 반환
     */
    public Map<String, Object> getParticipantStatus(String roomId, String userId) {
        RoomParticipant participant = roomParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

        Map<String, Object> result = new HashMap<>();
        result.put("status", participant.getParticipantStatus());
        result.put("lastPingAt", participant.getLastPingAt());

        return result;
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

    /**
     * 세션 종료
     */
    public void endSession(String roomId, String userId) {
        RoomParticipant participant = roomParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참가자를 찾을 수 없습니다."));

        participant.updateStatus(RoomParticipant.ParticipantStatus.OFFLINE);
        roomParticipantRepository.save(participant);
    }
    /**
     * 세션 상태를 IN_PROGRESS로 변경
     */
    @Transactional
    public Map<String, Object> updateSessionToInProgress(Integer sessionId) {


        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {

            throw new RuntimeException("세션을 찾을 수 없습니다: " + sessionId);
        }

        Session session = sessionOpt.get();

        // 상태 검증 (SCHEDULED 또는 WAITING만 IN_PROGRESS로 변경 가능)
        if (session.getSessionStatus() != Session.SessionStatus.SCHEDULED &&
                session.getSessionStatus() != Session.SessionStatus.WAITING) {

            throw new IllegalStateException("현재 상태에서는 면접을 시작할 수 없습니다: " + session.getSessionStatus());
        }

        // 상태 변경
        session.setSessionStatus(Session.SessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionStatus", session.getSessionStatus().name());
        result.put("message", "세션이 성공적으로 시작되었습니다.");



        return result;
    }

    /**
     * 세션 상태를 COMPLETED로 변경
     */
    @Transactional
    public Map<String, Object> updateSessionToCompleted(Integer sessionId) {


        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {

            throw new RuntimeException("세션을 찾을 수 없습니다: " + sessionId);
        }

        Session session = sessionOpt.get();

        // 상태 검증 (IN_PROGRESS만 COMPLETED로 변경 가능)
        if (session.getSessionStatus() != Session.SessionStatus.IN_PROGRESS) {

            throw new IllegalStateException("진행 중인 세션만 완료할 수 있습니다: " + session.getSessionStatus());
        }

        // 상태 변경
        session.setSessionStatus(Session.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionStatus", session.getSessionStatus().name());
        result.put("message", "세션이 성공적으로 완료되었습니다.");



        return result;
    }
    /**
     * 세션의 middleReviewText만 조회 - Map 직접 반환
     */
    @Transactional(readOnly = true)
    public Map<String, String> getMiddleReviewTexts(Integer sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));

        List<String> applicantIds = parseUserIds(session.getApplicantsUserId());

        if (applicantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<InterviewContent> interviewContents =
                interviewContentRepository.findByApplicantIds(applicantIds);

        return interviewContents.stream()
                .collect(Collectors.toMap(
                        content -> content.getApplicant().getApplicantId(),
                        content -> content.getMiddleReviewText() != null ? content.getMiddleReviewText() : ""
                ));
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
}