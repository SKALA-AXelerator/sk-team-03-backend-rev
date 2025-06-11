package com.skala03.skala_backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala03.skala_backend.global.config.VitoApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VitoAuthService {

    private final WebClient webClient;
    private final VitoApiProperties vitoApiProperties;
    private final ObjectMapper objectMapper;

    public Mono<String> getAccessToken() {
        return webClient.post()
                .uri("/v1/authenticate")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters
                        .fromFormData("client_id", vitoApiProperties.getClientId())
                        .with("client_secret", vitoApiProperties.getClientSecret())
                )
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 호출 실패: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .doOnNext(rawBody -> System.out.println("Raw Body: " + rawBody))
                .map(body -> {
                    try {
                        TokenResponse tokenResponse = objectMapper.readValue(body, TokenResponse.class);
                        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                            throw new RuntimeException("AccessToken이 비어있음");
                        }
                        return tokenResponse.getAccessToken();
                    } catch (Exception e) {
                        throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
                    }
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expire_at")
        private Long expireAt;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(Long expireAt) {
            this.expireAt = expireAt;
        }
    }
}