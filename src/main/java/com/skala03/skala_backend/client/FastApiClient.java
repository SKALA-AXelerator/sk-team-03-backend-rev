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
import com.skala03.skala_backend.dto.InterviewProcessingDto;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;  // ← 추가
import java.util.Map;

@Component
@Slf4j
public class FastApiClient {

    private WebClient webClient;

    @Value("${fastapi.base-url:https://intervia.skala25a.project.skala-ai.com}")
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
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB로 증가
                .build();
    }

    // ===== 기존 키워드 생성 메서드 =====
    public FastApiResponse generateKeywordCriteria(FastApiRequest request) {
        try {
            // job_role_id 제거 후 로그 수정
            log.info("FastAPI 호출 시작: keywordName={}", request.getKeywordName());

            FastApiResponse response = webClient.post()
                    .uri("/ai/generate-keyword-criteria")
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

    // ===== 🆕 새로 추가: Full Pipeline 메서드 =====
    /**
     * FastAPI full-pipeline 엔드포인트 호출
     */
    public FastApiPipelineResponse callFullPipeline(InterviewProcessingDto.FastApiRequest request) {
        try {
            log.info("📤 FastAPI full-pipeline 호출 시작: sessionId={}, 지원자수={}",
                    request.getSessionId(), request.getApplicantIds().size());

            FastApiPipelineResponse response = webClient.post()
                    .uri("/ai/full-pipeline")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.value() >= 400,
                            clientResponse -> {
                                log.error("FastAPI 오류: status={}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .defaultIfEmpty("No error body")
                                        .doOnNext(body -> log.error("FastAPI 오류 응답: {}", body))
                                        .flatMap(body -> Mono.error(new RuntimeException("FastAPI 호출 실패 (status: " +
                                                clientResponse.statusCode() + "): " + body)));
                            }
                    )
                    .bodyToMono(FastApiPipelineResponse.class)
                    .timeout(Duration.ofMinutes(10))  // 면접 처리는 시간이 오래 걸릴 수 있음
                    .doOnSuccess(res -> {
                        if (res != null && res.isSuccess()) {
                            log.info("✅ FastAPI 호출 성공: sessionId={}, 성공/실패={}/{}, 처리시간={}초",
                                    res.getSessionId(), res.getSuccessfulCount(), res.getFailedCount(), res.getTotalProcessingTime());
                        }
                    })
                    .doOnError(error -> log.error("❌ FastAPI 호출 오류: ", error))
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
            throw new RuntimeException("면접 처리 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }

    // 헬스체크 메서드
    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri("/ai/health2")  // health2로 변경
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

    // ===== 기존 키워드 생성용 DTO =====
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

    // ===== 🆕 새로 추가: Full Pipeline용 DTO =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiPipelineResponse {
        private boolean success;
        private String message;

        @JsonProperty("session_id")
        private Integer sessionId;

        @JsonProperty("raw_stt_s3_path")
        private String rawSttS3Path;

        @JsonProperty("job_role_name")
        private String jobRoleName;

        @JsonProperty("evaluation_results")
        private List<ApplicantResult> evaluationResults;

        @JsonProperty("total_processed")
        private Integer totalProcessed;

        @JsonProperty("successful_count")
        private Integer successfulCount;

        @JsonProperty("failed_count")
        private Integer failedCount;

        @JsonProperty("total_processing_time")
        private Double totalProcessingTime;

        @JsonProperty("step_times")
        private Map<String, Double> stepTimes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantResult {
        @JsonProperty("applicant_id")
        private String applicantId;

        @JsonProperty("applicant_name")
        private String applicantName;

        @JsonProperty("qna_s3_path")
        private String qnaS3Path;

        @JsonProperty("pdf_s3_path")
        private String pdfS3Path;

        @JsonProperty("evaluation_json")
        private Map<String, Object> evaluationJson;
    }
}