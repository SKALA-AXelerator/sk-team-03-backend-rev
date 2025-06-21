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

    public Mono<String> transcribe(File audioFile, String jwtToken, Integer speakerCount) {
        FileSystemResource fileResource = new FileSystemResource(audioFile);

        // 기본값 설정 (파라미터가 없으면 6명)
        int spkCount = (speakerCount != null) ? speakerCount : 6;

        // JSON 설정을 동적으로 생성
        String configJson = String.format("""
    {   "model_name": "sommers",
        "use_diarization": true,
        "diarization": {
            "spk_count": %d
        },
        "use_itn": false,
        "use_disfluency_filter": false,
        "use_profanity_filter": false,
        "use_paragraph_splitter": true,
        "paragraph_splitter": {
            "max": 50
        }
    }
    """, spkCount);

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

    public Mono<TranscriptionResult> transcribeAndPollResult(File audioFile, String jwtToken, Integer speakerCount) {
        return transcribe(audioFile, jwtToken, speakerCount)
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