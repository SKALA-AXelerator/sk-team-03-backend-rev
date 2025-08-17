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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import com.skala03.skala_backend.dto.interview.InterviewProcessingDto;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FastApiClient {

    private WebClient webClient;

    @Value("${fastapi.base-url:http://sk-team-03-ai-service.sk-team-03.svc.cluster.local:8000}")
    private String fastApiBaseUrl;

    @Value("${fastapi.api-key:internal-api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        // 연결 풀 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fastapi-pool")
                .maxConnections(30)
                .maxIdleTime(Duration.ofMinutes(5))
                .maxLifeTime(Duration.ofMinutes(10))
                .pendingAcquireTimeout(Duration.ofSeconds(20))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // HttpClient 전역 타임아웃 설정 (5분)
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000) // 연결 타임아웃: 15초
                .responseTimeout(Duration.ofMinutes(5)) // ✅ 전역 응답 타임아웃: 5분
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES))   // ✅ 전역 읽기 타임아웃: 5분
                                .addHandlerLast(new WriteTimeoutHandler(1, TimeUnit.MINUTES))); // 쓰기 타임아웃: 1분

        // WebClient 생성
        this.webClient = WebClient.builder()
                .baseUrl(fastApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-KEY", apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(70 * 1024 * 1024)) // 70MB
                .build();

        log.info("✅ FastAPI WebClient 초기화 완료: baseUrl={}, 전역타임아웃=5분", fastApiBaseUrl);
    }

    // ===== 키워드 생성 메서드 (타임아웃 제거) =====
    public FastApiResponse generateKeywordCriteria(FastApiRequest request) {
        try {
            log.info("FastAPI 키워드 생성 호출: keywordName={}", request.getKeywordName());

            FastApiResponse response = webClient.post()
                    .uri("/ai/generate-keyword-criteria")
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
                    .bodyToMono(FastApiResponse.class)
                    // ✅ .timeout() 제거 - HttpClient 전역 설정 사용 (5분)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(3))
                            .filter(throwable -> !(throwable instanceof WebClientResponseException
                                    && ((WebClientResponseException) throwable).getStatusCode().is4xxClientError())))
                    .doOnSuccess(res -> {
                        if (res != null && res.isSuccess()) {
                            log.info("✅ 키워드 생성 성공: keywordName={}, 기준수={}",
                                    res.getKeywordName(), res.getCriteria() != null ? res.getCriteria().size() : 0);
                        }
                    })
                    .doOnError(error -> log.error("❌ 키워드 생성 오류: ", error))
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI 응답이 null입니다.");
            }

            return response;

        } catch (Exception e) {
            log.error("❌ FastAPI 키워드 생성 클라이언트 오류: ", e);
            throw new RuntimeException("AI 키워드 생성 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }

    // ===== Full Pipeline 메서드 (타임아웃 제거) =====
    public FastApiPipelineResponse callFullPipeline(InterviewProcessingDto.FastApiRequest request) {
        try {
            log.info(" FastAPI full-pipeline 호출 시작: sessionId={}, 지원자수={}",
                    request.getSessionId(), request.getApplicantIds().size());

            FastApiPipelineResponse response = webClient.post()
                    .uri("/ai/full-pipeline")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.value() >= 400,
                            clientResponse -> {
                                log.error("FastAPI Pipeline 오류: status={}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .defaultIfEmpty("No error body")
                                        .doOnNext(body -> log.error("FastAPI Pipeline 오류 응답: {}", body))
                                        .flatMap(body -> Mono.error(new RuntimeException("FastAPI Pipeline 실패 (status: " +
                                                clientResponse.statusCode() + "): " + body)));
                            }
                    )
                    .bodyToMono(FastApiPipelineResponse.class)
                    //  .timeout() 제거 - HttpClient 전역 설정 사용 (5분)
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(10)) // 재시도 1회
                            .filter(throwable -> {
                                // 4xx 에러는 재시도하지 않음
                                if (throwable instanceof WebClientResponseException) {
                                    WebClientResponseException ex = (WebClientResponseException) throwable;
                                    return !ex.getStatusCode().is4xxClientError();
                                }
                                // 네트워크 오류, 타임아웃 등은 재시도
                                return true;
                            })
                            .doBeforeRetry(retrySignal ->
                                    log.warn(" FastAPI Pipeline 재시도: attempt={}, error={}",
                                            retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                    .doOnSuccess(res -> {
                        if (res != null && res.isSuccess()) {
                            log.info("✅ FastAPI Pipeline 성공: sessionId={}, 성공/실패={}/{}, 처리시간={}초",
                                    res.getSessionId(), res.getSuccessfulCount(), res.getFailedCount(),
                                    res.getTotalProcessingTime());
                        } else if (res != null) {
                            log.warn("⚠️ FastAPI Pipeline 부분 실패: sessionId={}, message={}",
                                    res.getSessionId(), res.getMessage());
                        }
                    })
                    .doOnError(error -> {
                        log.error("❌ FastAPI Pipeline 오류: sessionId={}, error={}",
                                request.getSessionId(), error.getMessage());
                    })
                    .block();

            if (response == null) {
                throw new RuntimeException("FastAPI Pipeline 응답이 null입니다.");
            }

            return response;

        } catch (Exception e) {
            log.error("❌ FastAPI Pipeline 클라이언트 오류: sessionId={}", request.getSessionId(), e);
            throw new RuntimeException("면접 처리 서비스 호출에 실패했습니다: " + e.getMessage());
        }
    }

    // 헬스체크 메서드 (타임아웃 제거)
    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri("/ai/health2")
                    .retrieve()
                    .bodyToMono(String.class)
                    // ✅ .timeout() 제거 - HttpClient 전역 설정 사용 (5분)
                    .block();

            log.debug("✅ FastAPI 헬스체크 성공: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("❌ FastAPI 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    // ===== DTO 클래스들 =====
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
        private Map<Integer, String> criteria;

        @JsonProperty("keyword_name")
        private String keywordName;

        @JsonProperty("error_detail")
        private String errorDetail;
    }

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