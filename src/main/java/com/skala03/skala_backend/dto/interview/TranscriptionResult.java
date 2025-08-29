package com.skala03.skala_backend.dto.interview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionResult {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("results")
    private TranscriptionResults results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TranscriptionResults {
        @JsonProperty("utterances")
        private List<Utterance> utterances;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Utterance {
        @JsonProperty("spk")
        private Integer spk;

        @JsonProperty("msg")
        private String msg;

        @JsonProperty("start_at")
        private Long startAt;

        @JsonProperty("duration")
        private Long duration;

        @JsonProperty("lang")
        private String lang;
    }
}
