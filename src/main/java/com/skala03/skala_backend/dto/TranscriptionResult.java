package com.skala03.skala_backend.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)  // ⭐️
public class TranscriptionResult {
    private String id;
    private String status;
    private Results results;

    @Data
    public static class Results {
        private List<Utterance> utterances = new ArrayList<>();
    }

    @Data
    public static class Utterance {

        private String msg;
        private int spk;

    }
}