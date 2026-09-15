# 01. 아키텍처

## 서비스의 책임 범위

Cultivation Server는 **"재배지에서 일어나는 일"**을 담당합니다.
재배지를 만들고, 누가 참여하는지 관리하고, 어떤 센서가 붙어 있고 어떤 환경을 유지해야 하는지 정하고,
사진과 수확 기록을 남기고, 실제 환경이 기준을 얼마나 지켰는지 계산합니다.

반대로 **하지 않는 일**을 명확히 해두는 편이 이해에 도움이 됩니다.

| 하지 않는 일 | 담당 서비스 |
| --- | --- |
| 로그인 · JWT 발급 · 회원 정보 관리 | User Server |
| 센서 값 수집 · 임계값 판정 · 액추에이터 제어 | Rule Engine Server |
| 생육 분석 · 리포트 생성 · 챗봇 | AI Server |
| 이미지 판정(YOLO) | Vision Server |
| 알림 발송(Telegram · Discord) | Notification Server |
| 라우팅 · 인증 검증 | API Gateway |

즉 이 서비스는 **판정하거나 분석하지 않고, 판정과 분석에 필요한 기준과 맥락을 제공**합니다.
Rule Engine이 쓸 임계값을 내려주고, AI Server가 쓸 사진과 수확 기록을 보관하는 식입니다.

## 요청이 들어오는 경로

```
Client
  └─ Cloudflare
      └─ Traefik Ingress
          └─ API Gateway        ← 여기서 JWT 검증 후 X-User-Id 헤더 주입
              └─ Cultivation Server
```

Cultivation Server는 인증을 직접 하지 않습니다.
Gateway가 주입한 `X-User-Id` 헤더를 신뢰하고, **재배지 멤버십과 역할만** 검증합니다.
자세한 내용은 [04. 인증과 권한](04-authorization.md)에 있습니다.

## 저장소 분리

한 서비스가 네 종류의 저장소를 쓰는 이유는 데이터 성격이 다르기 때문입니다.

| 저장소 | 저장 대상 | 이유 |
| --- | --- | --- |
| **PostgreSQL** | 재배·멤버·수확·센서 메타데이터·환경 설정 | 관계와 트랜잭션이 필요한 데이터 |
| **InfluxDB** | 센서 측정값 시계열 | 시간축 집계 질의가 잦고 데이터 양이 큼 |
| **Redis** | 최신 센서값 캐시, presigned URL 캐시, 분산 락 | 짧은 TTL과 빠른 읽기가 필요 |
| **MinIO** | 생육 사진 원본 | 바이너리 대용량 객체 |

PostgreSQL은 **서비스별 스키마 분리** 전략을 씁니다.
같은 데이터베이스 인스턴스를 쓰더라도 `CULTIVATION_DB_SCHEMA`로 지정한 스키마 안에서만 동작하므로,
다른 서비스의 테이블에 직접 접근하지 않습니다. 다른 서비스의 데이터가 필요하면 Feign이나 이벤트를 씁니다.

## 계층 구조

```
Controller  ──  요청 검증, 헤더에서 userId 추출, 응답 변환만
    │           (Swagger 애너테이션은 controller/docs 인터페이스로 분리)
    ▼
Facade      ──  여러 도메인에 걸친 흐름 조립 (선택적)
    │
    ▼
Service     ──  인터페이스 정의, impl/ 패키지에 구현
    │           트랜잭션 경계, 도메인 규칙, 이벤트 발행
    ▼
Repository  ──  JPA + QueryDSL. 복잡한 조회는 *QueryRepository 로 분리
    │
    ▼
Entity      ──  상태 변경 메서드를 엔티티 안에 둠 (setter 지양)
```

### Controller — 얇게 유지

컨트롤러는 HTTP 관심사만 다룹니다. Swagger 애너테이션(`@Operation`, `@ApiResponse` 등)은
`controller/docs` 패키지의 `*ControllerDocs` 인터페이스에 모아두고 컨트롤러가 이를 구현합니다.
덕분에 컨트롤러 본문은 위임 코드만 남습니다.

```
CultivationController implements CultivationControllerDocs
                                 └─ @Tag, @Operation, @Parameter 가 여기에
```

### Facade — 도메인 조합

여러 도메인을 오가는 흐름은 Facade로 묶습니다.
예를 들어 수확 모드 전환은 재배 상태 변경(cultivation 도메인)과
환경 임계값 재적용(sensor 도메인)을 함께 해야 하므로 `CultivationModeFacade`가 조립합니다.

| Facade | 조합하는 흐름 |
| --- | --- |
| `CultivationCreationFacade` | 환경 설정 검증·정규화 → 재배 생성 → 환경 설정 적용 → `ThresholdInfoEvent` 발행 |
| `CultivationModeFacade` | 수확 모드 전환 → 버섯 레퍼런스의 수확용 임계값을 등록된 센서 타입에 한해 재적용 |
| `CultivationSensorFacade` | 센서 등록·삭제·전체 삭제 → 임계값 이벤트와 센서 정보 이벤트 발행 |

이벤트는 `ApplicationEventPublisher`로 먼저 발행하고,
`@TransactionalEventListener`를 구현한 Producer가 커밋 이후 실제 RabbitMQ로 내보냅니다.
덕분에 트랜잭션이 롤백되면 메시지도 나가지 않습니다.

### 공통 가드

"재배지가 존재하는가 + 요청자가 멤버인가"는 거의 모든 API에서 필요합니다.
이 패턴은 `CultivationAccessGuard`에 모아두고 각 서비스가 재사용합니다.

```java
Cultivation cultivation = accessGuard.requireMember(cultivationId, userId, role);
```

## 예외 처리

도메인 예외는 각 패키지의 `exception/`에 두고, 공통 모듈(`yesaido-common`)의
상위 예외 타입을 상속합니다. `GlobalExceptionHandler`가 상위 타입별로 HTTP 상태를 매핑합니다.

| 공통 예외 타입 | HTTP 상태 |
| --- | --- |
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `UnsupportedMediaTypeException` | 415 |
| `CustomServerException` | 500 |

응답은 RFC 7807 `application/problem+json` 형식으로, `status` · `title` · `detail` · `code` · `instance`를 포함합니다.

새 예외를 만들 때는 적절한 상위 타입을 골라 상속하기만 하면 핸들러가 자동으로 처리합니다.
`GlobalExceptionHandler`에 케이스를 추가할 필요는 없습니다.

## 비동기 통신

상태 변화를 다른 서비스에 알릴 때는 RabbitMQ를 씁니다.
발행자는 `rabbitmq/*Producer`, 구독자는 `rabbitmq/*Listener`, 메시지 정의는 `rabbitmq/event/`의 `record`입니다.

설계 원칙은 **"상태가 바뀌면 예외 상황까지 포함해 반드시 이벤트로 발행하고, 판단은 수신 측이 한다"**입니다.
예를 들어 센서 데이터가 들어오지 않는 상황도 `SensorDataUnavailableEvent`로 발행하고,
그걸 알림으로 보낼지 말지는 Notification Server가 정합니다.

자세한 계약은 [05. 메시징](05-messaging.md)에 있습니다.

## 내부 API 분리

서비스 간 호출 전용 엔드포인트는 `/api/v1/internal/**` 경로에 두고
`controller/internal` 패키지에 배치합니다. 이 경로는 `springdoc.paths-to-exclude` 설정으로
Swagger 문서에서 제외되므로, 외부 사용자에게 노출되지 않습니다.

## 새 기능을 추가할 때

1. 도메인이 재배 자체와 관련되면 `cultivation/`, 센서·환경값과 관련되면 `sensor/` 아래에 둡니다.
2. 엔티티에 상태 변경 메서드를 추가합니다(무분별한 setter 대신).
3. 서비스 인터페이스를 정의하고 `impl/`에 구현합니다.
4. 여러 도메인을 건드리면 Facade를 만들거나 기존 Facade에 넣습니다.
5. 컨트롤러를 만들고, 같은 이름의 `*ControllerDocs` 인터페이스에 Swagger 애너테이션을 작성합니다.
6. 상태가 바뀌는 동작이라면 이벤트 발행이 필요한지 검토합니다.
7. 관련 문서를 같은 PR에서 갱신합니다.
