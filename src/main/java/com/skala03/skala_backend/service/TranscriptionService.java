package com.skala03.skala_backend.service;



import com.skala03.skala_backend.dto.TranscriptionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final WebClient webClient;

    public Mono<String> transcribe(File audioFile, String jwtToken) {
        FileSystemResource fileResource = new FileSystemResource(audioFile);

        String configJson = """
        {   "model_name": "sommers",
            "use_diarization": true,
            "diarization": {
                "spk_count": 6
            },
            "use_itn": false,
            "use_disfluency_filter": false,
            "use_profanity_filter": false,
            "use_paragraph_splitter": true,
            "paragraph_splitter": {
                "max": 50
            }
        }
        """; // ✅ spk_count: 2로 줄임 (권장)

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", fileResource);
        formData.add("config", configJson);

        return webClient.post()
                .uri("/v1/transcribe")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<TranscriptionResult> transcribeAndPollResult(File audioFile, String jwtToken) {
        return transcribe(audioFile, jwtToken)
                .flatMap(response -> {
                    String transcribeId = extractIdFromResponse(response);
                    return pollUntilComplete(transcribeId, jwtToken, 30);
                });
    }

    private String extractIdFromResponse(String response) {
        int idStart = response.indexOf(":\"") + 2;
        int idEnd = response.indexOf("\"", idStart);
        return response.substring(idStart, idEnd);
    }

    private Mono<TranscriptionResult> pollUntilComplete(String transcribeId, String jwtToken, int maxRetries) {
        return Mono.defer(() -> getTranscribeResult(transcribeId, jwtToken))
                .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofSeconds(5)).take(maxRetries))
                .timeout(Duration.ofMinutes(5));
    }

    public Mono<TranscriptionResult> getTranscribeResult(String transcribeId, String jwtToken) {
        return webClient.get()
                .uri("/v1/transcribe/" + transcribeId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(TranscriptionResult.class)
                .doOnNext(result -> System.out.println("Vito 응답 결과: " + result))  // ✅ Debug용 로그
                .filter(result -> "completed".equals(result.getStatus())); // 완료된 것만 통과
    }
}