package com.skala03.skala_backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class FastApiClient {

    private WebClient webClient;

    @Value("${fastapi.base-url:http://sk-team-03-ai-service:8000}")
    private String fastApiBaseUrl;

    @Value("${fastapi.api-key:internal-api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        // FastAPI 전용 WebClient 생성 (기존 WebClient와 분리)
        this.webClient = WebClient.builder()
                .baseUrl(fastApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-KEY", apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    public FastApiResponse generateKeywordCriteria(FastApiRequest request) {
        try {
            // job_role_id 제거 후 로그 수정
            log.info("FastAPI 호출 시작: keywordName={}", request.getKeywordName());

            FastApiResponse response = webClient.post()
                    .uri("/api/ai/generate-keyword-criteria")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.value() >= 400, // 400 이상의 모든 에러 상태 코드
                            clientResponse -> {
                                log.error("FastAPI 오류: status={}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .defaultIfEmpty("No error body")
                                        .doOnNext(body -> log.error("FastAPI 오류 응답: {}", body))
                                        .flatMap(body -> Mono.error(new RuntimeException("FastAPI 호출 실패 (status: " +
                                                clientResponse.statusCode() + "): " + body)));
                            }
                    )
                    .bodyToMono(FastApiResponse.class)
                    .timeout(Duration.ofMinutes(3))
                    .doOnSuccess(res -> {
                        if (res != null && res.isSuccess()) {
                            log.info("FastAPI 호출 성공: keywordName={}, 생성된 기준 수={}",
                                    res.getKeywordName(), res.getCriteria() != null ? res.getCriteria().size() : 0);
                        }
                    })
                    .doOnError(error -> log.error("FastAPI 호출 오류: ", error))
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI 응답이 null입니다.");
            }

            return response;

        } catch (WebClientResponseException e) {
            log.error("FastAPI WebClient 응답 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("FastAPI 호출 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("FastAPI 클라이언트 오류: ", e);
            throw new RuntimeException("AI 키워드 생성 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }

    // 헬스체크 메서드
    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.debug("FastAPI 헬스체크 성공: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("FastAPI 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    // FastAPI 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiRequest {
        @JsonProperty("keyword_name")
        private String keywordName;

        @JsonProperty("keyword_detail")
        private String keywordDetail;
    }

    // FastAPI 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiResponse {
        private boolean success;
        private String message;
        private Map<Integer, String> criteria; // score -> guideline 매핑

        @JsonProperty("keyword_name")
        private String keywordName;

        @JsonProperty("error_detail")
        private String errorDetail;
    }
}