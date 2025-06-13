# Multi-stage build for optimization
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Gradle wrapper와 build 파일들 먼저 복사 (캐시 최적화)
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# Build 설정 파일들 복사
COPY build.gradle ./
COPY settings.gradle ./

# 의존성 다운로드 (별도 레이어로 캐싱)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim

# 보안을 위한 non-root 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# 필요한 패키지 설치 및 정리
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        dumb-init && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/app.jar app.jar

# 파일 소유권 변경
RUN chown -R appuser:appuser /app

# non-root 사용자로 전환
USER appuser

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# JVM 옵션 최적화
ENV JVM_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# dumb-init을 사용하여 시그널 처리 개선
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JVM_OPTS -jar app.jar --spring.profiles.active=k8s"]