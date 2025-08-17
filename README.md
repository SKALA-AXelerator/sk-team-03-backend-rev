# 📋 대면 면접관 시스템 - Spring Boot 백엔드

면접 분석 및 관리 시스템의 Spring Boot 백엔드 API 서버입니다.

## 🔧 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.0
- **Database**: MariaDB (Production), H2 (Development)
- **Authentication**: JWT + Spring Security
- **Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Gradle

## 📁 프로젝트 구조

```
src/main/java/com/skala03/skala_backend/
├── controller/          # REST API 컨트롤러
│   ├── admin/          # 관리자 API
│   ├── applicant/      # 지원자 API
│   ├── auth/           # 인증 API
│   └── interview/      # 면접 API
├── service/            # 비즈니스 로직
├── repository/         # 데이터 접근 계층
├── entity/             # JPA 엔티티
├── dto/                # 데이터 전송 객체
├── global/             # 글로벌 설정
└── config/             # 설정 클래스
```
## 📁 인프라 구조
<img width="1230" height="952" alt="image" src="https://github.com/user-attachments/assets/1e4bb4d3-8db8-4342-a1f0-dd78e87d812f" />


## 🚀 실행 조건 

### 사전 요구사항
- **Java 17** 이상
- **MariaDB** 

### 환경 설정

`application.yml` 파일 설정:

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:mariadb://localhost:3306/skala_db
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: your-jwt-secret-key
  expiration: 86400000
```

### 실행 방법

```bash
# 개발 환경 실행
./gradlew bootRun

# JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/app.jar
```

## 📡 주요 API 엔드포인트

### 인증 API (`/api/auth`)
- `POST /login` - 로그인
- `POST /refresh` - 토큰 갱신
- `POST /logout` - 로그아웃

### 지원자 API (`/api/applicants`)
- `GET /` - 전체 지원자 조회
- `POST /questions` - 지원자별 질문 조회
- `POST /evaluations` - 지원자 평가 조회
- `PUT /status/interview-complete` - 면접 완료 처리

### 관리자 API (`/api/admin`)
- `GET /job-roles` - 직무 목록 조회
- `GET /keywords` - 키워드 목록 조회
- `POST /create-keywords` - 키워드 생성
- `GET /results/{job_role_id}` - 직무별 평가 결과 조회

### 면접 세션 API (`/api/interviewers`)
- `POST /start` - 면접 시작
- `PUT /status/{sessionId}/complete` - 면접 완료
- `POST /upload-audio` - 음성 파일 업로드
- `GET /transcription/{transcriptionId}` - 전사 결과 조회

## 📊 주요 데이터베이스 테이블

- **users**: 사용자 정보 (ADMIN, INTERVIEWER)
- **job_roles**: 직무 정보
- **applicants**: 지원자 정보
- **sessions**: 면접 세션 정보
- **evaluations**: 평가 결과

## 🧪 테스트 및 문서

### API 문서
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 테스트 실행
```bash
./gradlew test
```

### 헬스체크
- `GET /actuator/health`
## ⚡ Kubernetes 배포

### 기본 배포 파일

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: spring-backend
  template:
    metadata:
      labels:
        app: spring-backend
    spec:
      containers:
      - name: spring-backend
        image: spring-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: spring-backend-service
spec:
  selector:
    app: spring-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer
```

### 배포 명령어

```bash
# 배포
kubectl apply -f k8s/deployment.yaml

# 상태 확인
kubectl get pods
kubectl get services

# 로그 확인
kubectl logs -f deployment/spring-backend

# 포트 포워딩 (로컬 테스트)
kubectl port-forward service/spring-backend-service 8080:8080

```
## 🐳 Docker 배포

```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 이미지 빌드
docker build -t spring-backend .

# 컨테이너 실행
docker run -p 8080:8080 spring-backend
```

