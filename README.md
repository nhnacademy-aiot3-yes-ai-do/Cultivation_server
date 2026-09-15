# 🍄 Cultivation Server

Yesaido 스마트팜 플랫폼에서 **재배지(Cultivation) 도메인**을 담당하는 마이크로서비스입니다.
재배 생성부터 수확 종료까지의 생애주기, 재배지 멤버 권한, 생육 사진, 센서 등록과 환경 임계값,
그리고 InfluxDB 시계열 데이터를 이용한 환경 준수율 계산을 책임집니다.

```
Client → Cloudflare → Traefik Ingress → API Gateway → ┌─ Cultivation Server ─┐
                                                       │  PostgreSQL · Redis  │
                                                       │  InfluxDB  · MinIO   │
                                                       └──────────────────────┘
                                                            ↕ RabbitMQ ↕ Feign
                                          Rule Engine · AI Server · Notification · User Server
```

---

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [아키텍처 한눈에 보기](#아키텍처-한눈에-보기)
- [패키지 구조](#패키지-구조)
- [실행 방법](#실행-방법)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [테스트](#테스트)
- [배포](#배포)
- [상세 문서](#상세-문서)

---

## 주요 기능

| 영역 | 설명 |
| --- | --- |
| **재배 생애주기** | 재배 생성 → 진행 → 수확 모드 전환 → 종료 → 이력 조회. 삭제는 소프트 삭제(`deletedAt`) |
| **멤버 권한(RBAC)** | `OWNER` / `MANAGER` / `MEMBER` 3단계 역할, 멤버 초대·역할 변경·삭제, 소유권 위임 |
| **생육 사진** | MinIO 업로드, presigned URL 발급(Redis 캐시), 일자별 조회, AI 서버가 내부 API로 수집 |
| **수확 관리** | 수확량·메모 기록, 상위 백분위 기반 품질 등급(`TOP`/`HIGH`/`MID`/`LOW`) 산정 |
| **센서 관리** | 재배지별 센서 등록·삭제·조회, 재사용 가능한 센서 카탈로그, 센서 연결 상태 추적 |
| **환경 설정** | 재배지별 센서 타입 임계값(min/max) 관리, 수확 모드 전환 시 임계값 자동 재적용 |
| **환경 준수율** | InfluxDB 시계열을 집계해 온도·습도·CO₂·조도의 임계값 준수율 계산 (전체/일별/기간별) |
| **버섯 레퍼런스** | 버섯 종별 생육·수확 표준 환경값 관리, AI 서버 연동 재배 가이드 제공 |
| **이벤트 발행/구독** | 센서 정보·임계값 변경을 Rule Engine·Data Generator로 발행, 수확·멤버 추가 알림 발행 |

---

## 기술 스택

| 분류 | 사용 기술 |
| --- | --- |
| 언어 · 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.7, Spring Cloud 2025.1.2 |
| 빌드 | Maven (Wrapper 포함) |
| 관계형 DB | PostgreSQL (서비스별 스키마 분리), Flyway 마이그레이션 |
| 영속성 | Spring Data JPA (Hibernate), QueryDSL 7.2, Apache Commons DBCP2 |
| 시계열 DB | InfluxDB 2.x (`influxdb-client-java` 8.0.0) |
| 캐시 · 분산 락 | Redis (Spring Data Redis, Lua 스크립트) |
| 메시징 | RabbitMQ (Spring AMQP) |
| 오브젝트 스토리지 | MinIO 8.5.17 |
| 서비스 간 통신 | Spring Cloud OpenFeign |
| API 문서 | springdoc-openapi 3.1.1 (Swagger UI) |
| 공통 모듈 | `site.yesaido:yesaido-common:v1.2.3` |
| 테스트 | JUnit 5, Testcontainers 1.21.4, Spring REST Docs, Actuator(test 스코프) |
| 배포 | Docker (multi-stage), Kubernetes |

---

## 아키텍처 한눈에 보기

**인증은 이 서비스에서 하지 않습니다.** 모든 외부 요청은 API Gateway를 거치며,
Gateway가 JWT를 검증한 뒤 `X-User-Id`(필요 시 `X-User-Role`) 헤더를 주입합니다.
서비스는 이 헤더를 신뢰하고 재배지 멤버십과 역할만 검증합니다.

**데이터 저장소는 용도에 따라 나뉩니다.**
재배·멤버·수확·센서 메타데이터 같은 트랜잭션 데이터는 PostgreSQL에,
센서 측정값 같은 시계열 데이터는 InfluxDB에, 사진 원본은 MinIO에 둡니다.
Redis는 최신 센서값 캐시와 presigned URL 캐시, 스케줄러 분산 락에 사용합니다.

**다른 서비스와는 두 가지 방식으로 통신합니다.**
즉시 응답이 필요한 조회는 Feign(동기), 상태 변화 전파는 RabbitMQ(비동기)를 씁니다.
자세한 계약은 [메시징 문서](docs/05-messaging.md)와 [외부 연동 문서](docs/07-external-integration.md)를 참고하세요.

---

## 패키지 구조

```
site.yesaido.cultivation_server
├── config/                  # Swagger, RabbitMQ, InfluxDB, MinIO, QueryDSL, 센서 캐시 설정
├── exception/               # GlobalExceptionHandler (RFC 7807 problem+json)
├── rabbitmq/                # 이벤트 발행자(Producer) · 구독자(Listener) · 상수
│   ├── event/               # 이벤트 레코드 정의
│   └── test/                # 로컬 스모크 테스트용 엔드포인트
├── cultivation/             # 재배 도메인
│   ├── controller/          # 공개 API
│   │   ├── docs/            # Swagger 애너테이션 전용 인터페이스
│   │   └── internal/        # 서비스 간 내부 API (문서 노출 제외)
│   ├── service/             # 인터페이스 + impl/ 구현체
│   ├── repository/          # JPA · QueryDSL 리포지토리
│   ├── entity/              # 엔티티 · Enum
│   ├── dto/                 # 요청 · 응답 DTO
│   ├── client/              # Feign 클라이언트 (AI, User)
│   └── exception/           # 도메인 예외
└── sensor/                  # 센서 · 환경 설정 도메인
    ├── controller/          # 센서값 · 환경설정 · 준수율 · 레퍼런스 · 관리자 API
    ├── service/             # 센서 캐시 스케줄러, Influx 조회, 준수율 계산 등
    ├── repository/ entity/ dto/ mapper/ support/ validation/
    └── exception/
```

컨트롤러는 얇게 유지하고, Swagger 애너테이션은 `controller/docs`의 `*ControllerDocs` 인터페이스로 분리합니다.
컨트롤러가 이 인터페이스를 구현하므로 비즈니스 코드와 문서 애너테이션이 섞이지 않습니다.

서비스 계층은 인터페이스와 `impl` 구현체를 나누고, 여러 도메인을 조합하는 흐름은
`*Facade`(예: `CultivationModeFacade`, `CultivationSensorFacade`)로 묶습니다.

---

## 실행 방법

### 사전 요구사항

PostgreSQL, Redis, RabbitMQ, InfluxDB, MinIO가 실행 중이어야 합니다.
로컬에서는 Docker로 띄우는 것을 권장합니다. 자세한 구성은
[로컬 개발 문서](docs/08-local-development.md)를 참고하세요.

### 1. 환경 변수 준비

프로젝트 루트에 `.env` 파일을 만듭니다.
`application.yml`이 `optional:file:.env[.properties]`로 읽어들이므로 별도 설정은 필요 없습니다.

### 2. 실행

```bash
# 의존성 설치 및 빌드
./mvnw clean package

# 실행
./mvnw spring-boot:run

# 또는 빌드된 jar 실행
java -jar target/cultivation_server-0.0.1-SNAPSHOT.jar
```

### 3. 확인

```bash
open http://localhost:8080/swagger-ui.html
```

> **포트에 대해**
> `application.yml`에는 `server.port`가 없어 Spring Boot 기본값인 **8080**으로 뜹니다.
> `application-dev2.yml` 프로파일을 쓰면 **9001**이 됩니다.
> 한편 `SwaggerConfig`는 "로컬 직접 호출" 서버 항목을 만들 때 `${server.port:9001}`을 참조하므로,
> 기본 프로파일로 띄우면 Swagger 화면에 표시되는 로컬 주소만 9001로 나옵니다(실제 포트는 8080).
> 다른 포트를 쓰려면 `--server.port=<포트>`로 실행하세요.
>
> 헬스 체크용 Actuator는 **테스트 스코프로만** 추가되어 있어 실행 중인 서버에는 `/actuator/**`가 열리지 않습니다.

---

## 환경 변수

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_HOST` · `DB_PORT` · `DB_NAME` | PostgreSQL 접속 정보 | — (필수) |
| `DB_USERNAME` · `DB_PASSWORD` | PostgreSQL 인증 정보 | — (필수) |
| `CULTIVATION_DB_SCHEMA` | 이 서비스 전용 스키마 이름 | — (필수) |
| `REDIS_HOST` · `REDIS_PORT` | Redis 접속 정보 | `localhost` · `6379` |
| `REDIS_PASSWORD` · `REDIS_DATABASE` | Redis 인증 · DB 인덱스 | — |
| `RABBITMQ_HOST` · `RABBITMQ_PORT` | RabbitMQ AMQP 접속 정보 | — (필수) |
| `RABBITMQ_USERNAME` · `RABBITMQ_PASSWORD` | RabbitMQ 인증 정보 | — (필수) |
| `INFLUX_URL` · `INFLUX_ORG` · `INFLUX_TOKEN` | InfluxDB 접속 정보 | — (필수) |
| `INFLUX_BUCKET` | 센서 데이터 버킷 | `sensor-data` |
| `CF_ACCESS_CLIENT_ID` · `CF_ACCESS_CLIENT_SECRET` | InfluxDB 앞단 Cloudflare Access 자격 증명 | 빈 값 |
| `MINIO_URL` · `MINIO_ACCESS_KEY` · `MINIO_SECRET_KEY` · `MINIO_BUCKET` | MinIO 접속 정보 | — (필수) |
| `MINIO_PUBLIC_BASE_URL` | 사진 URL에 사용할 공개 프록시 주소 | — (필수) |
| `USER_SERVER_URL` · `AI_SERVER_URL` | Feign 대상 서비스 주소 | — (필수) |
| `SWAGGER_ENABLED` | Swagger UI · API Docs 노출 여부 | `true` |
| `LOG_LEVEL` | 루트 로그 레벨 | `INFO` |
| `EUREKA_ENABLED` | Eureka 사용 여부 (현재 미사용) | `false` |
| `FLYWAY_OUT_OF_ORDER` | 순서 밖 마이그레이션 허용 | `false` |

센서 캐시 동작을 조정하는 `SENSOR_CACHE_*` 변수는
[센서 파이프라인 문서](docs/06-sensor-pipeline.md#설정-값)에 따로 정리했습니다.

> **주의** `application.yml`의 datasource URL은 `${Cultivation_DB_SCHEMA}`,
> JPA·Flyway 설정은 `${CULTIVATION_DB_SCHEMA}`로 대소문자가 다르게 적혀 있습니다.
> 환경 변수는 대문자 `CULTIVATION_DB_SCHEMA` 하나로 두되, 스키마가 예상과 다르게 잡히면 이 부분을 먼저 확인하세요.

---

## API 문서

서버를 띄운 뒤 Swagger UI에서 전체 명세를 확인할 수 있습니다.

| 항목 | 경로 |
| --- | --- |
| Swagger UI | `/swagger-ui.html` |
| OpenAPI 3 JSON | `/v3/api-docs` |

문서에는 `/api/v1/**`만 포함되며, `/api/v1/internal/**`(서비스 간 내부 API)은 제외됩니다.
엔드포인트 전체 목록은 [API 문서](docs/03-api.md)에 정리되어 있습니다.

---

## 테스트

```bash
# 전체 테스트
./mvnw test

# 특정 테스트만
./mvnw test -Dtest=CultivationServiceImplTest
```

테스트는 Testcontainers로 실제 미들웨어를 띄워 실행합니다.
따라서 **로컬에서 테스트하려면 Docker가 실행 중**이어야 합니다.

CI(GitHub Actions)에서는 PostgreSQL 16, Redis 7.2, RabbitMQ 3.13, InfluxDB 2.7, MinIO
5개 서비스 컨테이너를 띄운 뒤 테스트와 커버리지(JaCoCo)를 측정합니다.

---

## 배포

`main` 브랜치에 푸시되면 GitHub Actions가 테스트·커버리지·SonarQube 품질 게이트를 통과시킨 뒤
GHCR에 이미지를 푸시하고, Config 저장소로 `repository_dispatch` 이벤트를 보내 중앙 배포를 위임합니다.

| 워크플로 | 트리거 | 역할 |
| --- | --- | --- |
| `pr-check.yml` | PR 생성 · 갱신 | 의존성 검사, 빌드·테스트, Docker 빌드 검증 |
| `pr-review-notify.yml` | PR 오픈 | Discord 웹훅 알림 |
| `develop-ci.yml` | `develop` 푸시 | 테스트 재실행 |
| `deploy.yml` | `main` 푸시 | 커버리지·품질 게이트 → GHCR 푸시 → Config 저장소로 배포 위임 |
| `_reusable-test.yml` | 위 워크플로에서 호출 | 서비스 컨테이너 기동 및 테스트 실행 |

컨테이너 이미지는 멀티 스테이지로 빌드하며, 실행 단계에서는
`eclipse-temurin:21-jre-alpine` 위에서 비루트 사용자(`spring`)로 구동합니다.

---

## 상세 문서

| 문서 | 내용 |
| --- | --- |
| [01. 아키텍처](docs/01-architecture.md) | 서비스 경계, 계층 구조, 설계 규칙 |
| [02. 도메인 모델](docs/02-domain-model.md) | 엔티티·ERD·상태 전이·Enum |
| [03. API 명세](docs/03-api.md) | 전체 엔드포인트 목록과 권한 요구사항 |
| [04. 인증과 권한](docs/04-authorization.md) | Gateway 헤더 규약, RBAC 규칙 |
| [05. 메시징](docs/05-messaging.md) | RabbitMQ 교환기·큐·이벤트 계약 |
| [06. 센서 파이프라인](docs/06-sensor-pipeline.md) | InfluxDB 조회, Redis 캐시, 환경 준수율 |
| [07. 외부 연동](docs/07-external-integration.md) | Feign 클라이언트, MinIO 사진 흐름 |
| [08. 로컬 개발](docs/08-local-development.md) | 실행 환경 구성, 트러블슈팅 |
