package com.skala03.skala_backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * @EnableAsync: 비동기 기능 활성화
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "transcriptionExecutor")
    public Executor transcriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // 기본 스레드 수
        executor.setMaxPoolSize(10);           // 최대 스레드 수
        executor.setQueueCapacity(25);         // 큐 용량
        executor.setThreadNamePrefix("Transcription-"); // 스레드 이름 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료시 작업 완료 대기
        executor.setAwaitTerminationSeconds(60); // 최대 대기 시간
        executor.initialize();
        return executor;
    }
}