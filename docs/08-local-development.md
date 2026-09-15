# 08. 로컬 개발

## 필요한 것

| 항목 | 버전 | 비고 |
| --- | --- | --- |
| JDK | 21 | `java -version`으로 확인 |
| Docker | 최신 | 미들웨어 구동 + Testcontainers 실행에 필수 |
| Maven | — | `./mvnw` 래퍼가 포함되어 있어 별도 설치 불필요 |

Cultivation Server는 PostgreSQL · Redis · RabbitMQ · InfluxDB · MinIO 다섯 가지에 의존합니다.
하나라도 없으면 기동 중 실패하므로 먼저 띄워두세요.

---

## 미들웨어 띄우기

프로젝트에 compose 파일이 포함되어 있지 않다면, 아래 구성을 참고해 로컬에 만들어 쓰세요.
(CI에서 사용하는 이미지 버전과 맞췄습니다.)

```yaml
# docker-compose.local.yml (예시)
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: yesaido
      POSTGRES_USER: yesaido
      POSTGRES_PASSWORD: yesaido
    ports: ["5432:5432"]

  redis:
    image: redis:7.2-alpine
    ports: ["6379:6379"]

  rabbitmq:
    image: rabbitmq:3.13-management
    ports: ["5672:5672", "15672:15672"]   # 15672 = 관리 UI

  influxdb:
    image: influxdb:2.7
    environment:
      DOCKER_INFLUXDB_INIT_MODE: setup
      DOCKER_INFLUXDB_INIT_USERNAME: admin
      DOCKER_INFLUXDB_INIT_PASSWORD: admin12345
      DOCKER_INFLUXDB_INIT_ORG: yesaido
      DOCKER_INFLUXDB_INIT_BUCKET: sensor-data
      DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: local-dev-token
    ports: ["8086:8086"]

  minio:
    image: bitnamilegacy/minio:2025.7.23-debian-12-r5
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    ports: ["9000:9000", "9001:9001"]
```

```bash
docker compose -f docker-compose.local.yml up -d
```

기동 후 PostgreSQL에 이 서비스가 쓸 스키마를 만들어 둡니다.

```sql
CREATE SCHEMA IF NOT EXISTS cultivation;
```

---

## `.env` 작성

프로젝트 루트에 `.env` 파일을 만듭니다.
`application.yml`이 `config.import: optional:file:.env[.properties]`로 읽어들이므로
별도 설정 없이 자동 적용됩니다. `.env`는 `.gitignore`에 등록되어 있어 커밋되지 않습니다.

```properties
# --- PostgreSQL ---
DB_HOST=localhost
DB_PORT=5432
DB_NAME=yesaido
DB_USERNAME=yesaido
DB_PASSWORD=yesaido
CULTIVATION_DB_SCHEMA=cultivation

# --- Redis ---
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# --- RabbitMQ ---
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# --- InfluxDB ---
INFLUX_URL=http://localhost:8086
INFLUX_ORG=yesaido
INFLUX_BUCKET=sensor-data
INFLUX_TOKEN=local-dev-token
CF_ACCESS_CLIENT_ID=
CF_ACCESS_CLIENT_SECRET=

# --- MinIO ---
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET=cultivation
MINIO_PUBLIC_BASE_URL=http://localhost:9000

# --- 연동 서비스 ---
USER_SERVER_URL=http://localhost:9002
AI_SERVER_URL=http://localhost:9003

# --- 기타 ---
LOG_LEVEL=DEBUG
EUREKA_ENABLED=false
SWAGGER_ENABLED=true
```

> MinIO 버킷(`MINIO_BUCKET`)은 미리 만들어 두어야 합니다.
> 관리 콘솔(`http://localhost:9001`)에서 생성하거나 `mc` CLI를 쓰세요.

---

## 실행

```bash
./mvnw clean package          # 빌드
./mvnw spring-boot:run        # 실행
```

기본 포트는 **8080**입니다(`application.yml`에 `server.port`가 없어 Spring Boot 기본값).
9001로 띄우려면 둘 중 하나를 쓰세요.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9001
# 또는 dev2 프로파일 사용
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev2
```

확인:

```
http://localhost:8080/swagger-ui.html
```

> Actuator는 테스트 스코프로만 추가되어 있어 `/actuator/health`는 열리지 않습니다.
> 기동 여부는 로그나 Swagger 페이지로 확인하세요.

---

## API 수동 호출

Gateway 없이 직접 호출할 때는 `X-User-Id` 헤더를 직접 넣어야 합니다.

```bash
curl -H "X-User-Id: 1" http://localhost:8080/api/v1/cultivations
```

관리자 권한이 필요한 조회는 역할 헤더를 추가합니다.

```bash
curl -H "X-User-Id: 1" -H "X-User-Role: ADMIN" \
     http://localhost:8080/api/v1/cultivations/1
```

프로젝트 루트의 `http/req.http` 파일에 미리 작성된 요청 모음이 있습니다.
IntelliJ HTTP Client나 VS Code REST Client로 바로 실행할 수 있고,
사진 업로드 테스트용 `http/sample.jpg`도 함께 들어 있습니다.

---

## 테스트

```bash
./mvnw test                                  # 전체
./mvnw test -Dtest=CultivationServiceImplTest # 특정 클래스
./mvnw test -Dtest=CultivationServiceImplTest#create_성공  # 특정 메서드
```

테스트는 **Testcontainers로 실제 미들웨어를 띄웁니다.**
따라서 Docker가 실행 중이어야 하고, 첫 실행 시 이미지를 받느라 시간이 걸립니다.

현재 테스트 파일은 76개이며, CI에서는 아래 5개 서비스 컨테이너 위에서 실행됩니다.

| 서비스 | 이미지 |
| --- | --- |
| PostgreSQL | `postgres:16` |
| Redis | `redis:7.2-alpine` |
| RabbitMQ | `rabbitmq:3.13-management` |
| InfluxDB | `influxdb:2.7` |
| MinIO | `bitnamilegacy/minio:2025.7.23-debian-12-r5` |

배포 워크플로는 JaCoCo 커버리지 40% 기준과 SonarQube 품질 게이트를 통과해야 진행됩니다.

---

## RabbitMQ 동작 확인

관리 UI(`http://localhost:15672`, 기본 `guest`/`guest`)에서 교환기와 큐가 선언되었는지 볼 수 있습니다.
애플리케이션이 기동하면서 `RabbitMQConfig`의 빈들이 자동으로 선언합니다.

발행이 잘 되는지 빠르게 확인하려면 스모크 테스트 엔드포인트를 쓰세요.

```
POST http://localhost:8080/internal/test/rabbit/sensor-created
```

자세한 사용법은 `src/main/java/.../rabbitmq/test/readme.md`에 있습니다.

---

## 트러블슈팅

### 기동 시 스키마를 못 찾음

`application.yml`에서 datasource URL은 `${Cultivation_DB_SCHEMA}`,
JPA·Flyway는 `${CULTIVATION_DB_SCHEMA}`로 **대소문자가 다르게** 적혀 있습니다.
환경 변수는 대문자 하나로 두되, 스키마가 예상과 다르게 잡히면 이 부분을 먼저 확인하세요.

### Flyway 마이그레이션 오류

이미 V1이 적용된 DB에 V0.1을 도입하는 경우에만 out-of-order가 필요합니다.

```properties
FLYWAY_OUT_OF_ORDER=true
```

평소에는 `false`로 두세요.

### 테스트가 컨테이너 시작에서 멈춤

Docker 데몬이 떠 있는지, 이미지 다운로드가 진행 중인지 확인하세요.
사내망·프록시 환경이면 Docker 레지스트리 접근 설정이 필요할 수 있습니다.

### 사진 URL이 열리지 않음

`MINIO_PUBLIC_BASE_URL`이 실제 접근 가능한 주소인지 확인하세요.
로컬에서는 `MINIO_URL`과 같은 값(`http://localhost:9000`)으로 두면 됩니다.
presigned URL은 30분 뒤 만료되므로, 오래된 URL을 재사용하면 403이 납니다.

### 센서값이 비어 있음

Rule Engine이 InfluxDB에 데이터를 적재해야 값이 보입니다.
Cultivation Server 단독으로는 센서값이 생성되지 않습니다.
캐시 상태가 `REDIS_PENDING`이면 워밍업 중이니 `poll-initial-delay-ms`(기본 10초)만큼 기다리세요.

### Feign 호출 실패

`USER_SERVER_URL` · `AI_SERVER_URL`이 가리키는 서비스가 떠 있어야 합니다.
멤버 목록 조회는 User Server에, 재배 가이드는 AI Server에 의존합니다.

---

## 코드 작성 시 참고

| 상황 | 참고 문서 |
| --- | --- |
| 새 기능을 어디에 둘지 | [01. 아키텍처](01-architecture.md#새-기능을-추가할-때) |
| 엔티티·필드 확인 | [02. 도메인 모델](02-domain-model.md) |
| 권한 검증 메서드 선택 | [04. 인증과 권한](04-authorization.md#검증-메서드) |
| 이벤트 추가 | [05. 메시징](05-messaging.md) |
| 캐시 동작 조정 | [06. 센서 파이프라인](06-sensor-pipeline.md#설정-값) |

컨트롤러를 추가할 때는 같은 이름의 `*ControllerDocs` 인터페이스에
Swagger 애너테이션을 작성하는 규칙을 지켜주세요.
