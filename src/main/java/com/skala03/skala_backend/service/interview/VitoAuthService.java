// VitoAuthService.java - 인증 서비스
package com.skala03.skala_backend.service.interview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala03.skala_backend.global.config.VitoApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class VitoAuthService {

    private final RestTemplate restTemplate;
    private final VitoApiProperties vitoApiProperties;
    private final ObjectMapper objectMapper;

    public String getAccessToken() {
        try {
            log.info("Vito 인증 토큰 요청 시작");

            String baseUrl = "https://openapi.vito.ai";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", vitoApiProperties.getClientId());
            formData.add("client_secret", vitoApiProperties.getClientSecret());

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            log.debug("Vito 인증 API 호출 - URL: {}/v1/authenticate", baseUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/v1/authenticate",
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Vito 인증 실패 - 상태코드: {}, 응답: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("API 인증 실패: " + response.getBody());
            }

            String body = response.getBody();
            log.debug("Vito 인증 응답: {}", body);

            TokenResponse tokenResponse = objectMapper.readValue(body, TokenResponse.class);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("Vito 토큰 응답이 비어있음");
                throw new RuntimeException("AccessToken이 비어있음");
            }

            log.info("Vito 인증 토큰 획득 성공");
            return tokenResponse.getAccessToken();

        } catch (Exception e) {
            log.error("Vito 토큰 획득 실패: {}", e.getMessage(), e);
            throw new RuntimeException("토큰 획득 실패: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expire_at")
        private Long expireAt;

        // getters and setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public Long getExpireAt() { return expireAt; }
        public void setExpireAt(Long expireAt) { this.expireAt = expireAt; }
    }
}