
package com.skala03.skala_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InterviewerResponse {

    private List<RoomInfo> roomList;

    @Getter
    @AllArgsConstructor
    public static class RoomInfo {
        private List<Interviewer> interviewers;
        private int totalCount;
        private int remainingCount;
    }

    @Getter
    @AllArgsConstructor
    public static class Interviewer {
        private String id;
        private String name;
    }
}