
package com.skala03.skala_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewerResponse {
    private List<RoomInfo> roomList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomInfo {
        private String roomId;           // 룸 ID 추가
        private String roomLeaderId;     // 룸 리더 ID 추가
        private List<Interviewer> interviewers;
        private int totalCount;          // 지원자 총 수
        private int remainingCount;      // 대기 중인 지원자 수
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Interviewer {
        private String id;      // userId
        private String name;    // userName
    }
}
