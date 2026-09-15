# 05. 메시징 (RabbitMQ)

Cultivation Server는 **상태 변화를 알릴 때** RabbitMQ를 씁니다.
즉시 응답이 필요한 조회는 Feign(동기)을 쓰므로, 여기 나오는 메시지는 모두 "알림" 성격입니다.

설계 원칙은 **"상태가 바뀌면 예외 상황까지 포함해 반드시 이벤트로 발행하고, 판단은 수신 측이 한다"**입니다.
센서 데이터가 끊긴 상황도 이벤트로 내보내고, 그걸 알림으로 보낼지는 Notification Server가 정합니다.

---

## 토폴로지

모든 이름은 `rabbitmq/RabbitMQConstants.java`에 상수로 정의되어 있습니다.

### 교환기

| 상수 | 이름 | 타입 | 용도 |
| --- | --- | --- | --- |
| `SENSOR_EXCHANGE` | `yes-nhn.sensor.exchange` | Topic | 센서 정보 · 임계값 변경 전파 |
| `HARVEST_EXCHANGE` | `yes-nhn.harvest.exchange` | Direct | 수확 완료를 AI 서버로 전달 |
| `NOTIFICATION_EXCHANGE` | `yes-nhn.notification.exchange` | Direct | 사용자 알림 발행 |
| `DLX_NAME` | `yes-nhn.dlx` | Fanout | 데드레터 교환기 |

### 큐

| 상수 | 이름 | 바인딩 | 소비자 |
| --- | --- | --- | --- |
| `RULE_ENGINE_SENSOR_INFO_QUEUE` | `yes-nhn.rule-engine.sensor-info.queue` | `yes-nhn.#.sensor-info.queue` | Rule Engine |
| `DATA_SOURCE_SENSOR_INFO_QUEUE` | `yes-nhn.data-source.sensor-info.queue` | `yes-nhn.#.sensor-info.queue` | Data Generator |
| `RULE_ENGINE_THRESHOLD_INFO_QUEUE` | `yes-nhn.rule-engine.threshold-info.queue` | `yes-nhn.#.threshold-info.queue` | Rule Engine |
| `DATA_SOURCE_THRESHOLD_INFO_QUEUE` | `yes-nhn.data-source.threshold-info.queue` | `yes-nhn.#.threshold-info.queue` | Data Generator |
| `ENVIRONMENT_COMPLIANCE_REQUEST_QUEUE` | `yes-nhn.environment.compliance.queue` | — | **Cultivation Server(수신)** |
| `AI_HARVEST_QUEUE` | `yes-nhn.ai.harvest.queue` | Direct | AI Server |
| `DLQ_QUEUE` | `yes-nhn.dlq` | DLX Fanout | 실패 메시지 보관 |

센서·임계값 큐가 와일드카드(`yes-nhn.#.sensor-info.queue`) 패턴으로 바인딩되어 있어,
**한 번 발행하면 Rule Engine과 Data Generator 양쪽이 모두 받습니다.**
새 소비자를 붙일 때도 같은 패턴의 큐를 만들어 바인딩하면 발행 코드는 건드릴 필요가 없습니다.

### 라우팅 키

| 상수 | 값 | 대상 |
| --- | --- | --- |
| `NOTIFICATION_HARVEST_ROUTING_KEY` | `yes-nhn.notification.harvest.queue` | 수확 완료 알림 |
| `NOTIFICATION_MEMBER_ROUTING_KEY` | `yes-nhn.notification.member.queue` | 멤버 추가 알림 |

모든 업무 큐는 `x-dead-letter-exchange` 인자로 DLX에 연결되어 있어,
처리에 실패한 메시지는 `yes-nhn.dlq`에 쌓입니다.

---

## 발행 방식

이벤트는 두 단계를 거칩니다.

```
Service  ──ApplicationEventPublisher──▶  Producer  ──RabbitTemplate──▶  RabbitMQ
         (트랜잭션 안)                   (커밋 이후)
```

Producer는 `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`로 동작합니다.
**트랜잭션이 롤백되면 메시지도 나가지 않고**, 발행이 요청 응답을 지연시키지도 않습니다.

---

## 발행하는 이벤트

### SensorInfoUpsertEvent — 센서 등록·수정

센서가 등록되거나 정보가 바뀌면 발행합니다. Rule Engine과 Data Generator가 받습니다.

| 필드 | 타입 | 비고 |
| --- | --- | --- |
| `cultivationId` | Long | 필수 |
| `location` · `locationDetail` | String | 설치 위치 |
| `deviceModel` · `deviceName` · `deviceEui` | String | 장치 정보 |
| `sensorType` · `unit` | String | 측정 타입과 단위 |
| `occurredAt` | OffsetDateTime | 발생 시각 |

발행자 `SensorInfoUpsertProducer` · 교환기 `yes-nhn.sensor.exchange`

### SensorInfoDeleteEvent — 센서 삭제

| 필드 | 타입 |
| --- | --- |
| `cultivationId` | Long |
| `deviceEui` · `sensorType` · `unit` | String |
| `occurredAt` | OffsetDateTime |

### ThresholdInfoEvent — 임계값 변경

가장 중요한 이벤트입니다. **재배 생성 · 환경 설정 수정 · 수확 모드 전환 · 재배 종료** 시 발행되며,
Rule Engine의 판정 기준이 이 값으로 갱신됩니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `cultivationId` | Long | 대상 재배지 |
| `sensorRangeList` | `List<SensorRange>` | 적용할 임계값 전체 목록 |
| `occurredAt` | OffsetDateTime | 발생 시각 |

`SensorRange`는 `sensorType` · `unit` · `minValue` · `maxValue`로 구성되며,
생성자에서 `null` 검증을 수행해 잘못된 메시지가 나가지 않도록 막습니다.

**빈 리스트의 의미가 특별합니다.** `sensorRangeList`가 비어 있으면
"해당 재배지의 임계값을 전부 삭제하고 판정을 중단하라"는 뜻입니다. 재배 종료 시 이 형태로 발행됩니다.

발행자 `ThresholdInfoProducer` · 교환기 `yes-nhn.sensor.exchange`

### SensorDataUnavailableEvent — 센서 데이터 미수신

센서에서 데이터가 들어오지 않을 때 발행합니다. 알림 여부는 수신 측이 판단합니다.

| 필드 | 타입 |
| --- | --- |
| `eventId` | UUID |
| `cultivationId` | long |
| `deviceName` · `message` | String |
| `occurredAt` | OffsetDateTime |

### HARVEST_COMPLETED — 수확 완료

수확이 등록되면 **두 곳으로 동시에** 발행합니다.

| 대상 | 교환기 / 라우팅 키 | 메시지 |
| --- | --- | --- |
| Notification Server | `yes-nhn.notification.exchange` / `yes-nhn.notification.harvest.queue` | `NotificationEvent<HarvestCompletedPayload>` |
| AI Server | `yes-nhn.harvest.exchange` / `yes-nhn.ai.harvest.queue` | `AiHarvestEvent` |

`AiHarvestEvent`는 `cultivationId` · `userId` · `cultivationName` · `harvestWeight`를 담으며,
`harvestWeight`가 `null`이면 `BigDecimal.ZERO`로 대체합니다.

발행은 **best-effort**입니다. 최대 3회 재시도(100ms 백오프) 후에도 실패하면
경고 로그만 남기고 포기합니다. 알림 실패가 수확 등록 자체를 되돌리지는 않습니다.

### MEMBER_ADDED — 멤버 추가

| 대상 | 교환기 / 라우팅 키 | 메시지 |
| --- | --- | --- |
| Notification Server | `yes-nhn.notification.exchange` / `yes-nhn.notification.member.queue` | `NotificationEvent<MemberAddedPayload>` |

`MemberAddedPayload`는 `cultivationId` · `cultivationName` · `role`을 담습니다.

---

## 알림 이벤트 공통 봉투

Notification Server로 보내는 메시지는 모두 `NotificationEvent<T>`로 감쌉니다.

```java
public record NotificationEvent<T>(
        String eventId,      // UUID
        String eventType,    // "HARVEST_COMPLETED", "MEMBER_ADDED"
        String producer,     // "cultivation-server"
        String targetType,   // "CULTIVATION"
        Long   targetId,     // cultivationId 또는 대상 사용자 ID
        String occurredAt,   // KST(UTC+9) ISO-8601 문자열
        T      payload
) {}
```

| `eventType` | `targetType` | `payload` |
| --- | --- | --- |
| `HARVEST_COMPLETED` | `CULTIVATION` | `HarvestCompletedPayload(cultivationName, harvestWeight)` |
| `MEMBER_ADDED` | — | `MemberAddedPayload(cultivationId, cultivationName, role)` |

`eventId`는 매 발행마다 새로 만드는 UUID이므로, 수신 측에서 멱등 처리 키로 쓸 수 있습니다.

---

## 구독하는 메시지

### EnvironmentComplianceRequest — 환경 준수율 요청 (RPC)

이 서비스가 **수신자**가 되는 유일한 외부 메시지입니다.
AI Server가 리포트를 만들 때 환경 준수율이 필요하면 이 큐로 요청을 보내고 응답을 받아갑니다.

| 항목 | 내용 |
| --- | --- |
| 큐 | `yes-nhn.environment.compliance.queue` |
| 리스너 | `EnvironmentComplianceRpcListener` |
| 요청 | `EnvironmentComplianceRequest(cultivationId, startDate, endDate)` |
| 응답 | `EnvironmentComplianceResponse(temperatureCompliance, humidityCompliance, co2Compliance, lightCompliance)` |

`@RabbitListener` 메서드가 값을 `return`하므로 Spring AMQP가 요청-응답(RPC) 패턴으로 처리합니다.
계산 로직은 [06. 센서 파이프라인](06-sensor-pipeline.md#환경-준수율)에 있습니다.

---

## 전체 흐름 요약

```
[재배 생성 / 환경설정 수정 / 수확모드 전환 / 재배 종료]
        └─ ThresholdInfoEvent ──▶ sensor.exchange ──┬──▶ Rule Engine
                                                     └──▶ Data Generator

[센서 등록 / 삭제]
        └─ SensorInfoUpsert·DeleteEvent ──▶ sensor.exchange ──┬──▶ Rule Engine
                                                               └──▶ Data Generator

[센서 데이터 미수신 감지]
        └─ SensorDataUnavailableEvent ──▶ (수신 측이 알림 여부 판단)

[수확 등록]
        ├─ NotificationEvent<HarvestCompleted> ──▶ notification.exchange ──▶ Notification
        └─ AiHarvestEvent ──────────────────────▶ harvest.exchange ──────▶ AI Server

[멤버 추가]
        └─ NotificationEvent<MemberAdded> ──────▶ notification.exchange ──▶ Notification

[AI 리포트 작성 중]
   AI Server ──EnvironmentComplianceRequest──▶ environment.compliance.queue
             ◀──EnvironmentComplianceResponse── Cultivation Server
```

---

## 로컬 스모크 테스트

RabbitMQ 발행이 제대로 동작하는지 빠르게 확인할 수 있는 테스트 엔드포인트가 있습니다.

```
POST /internal/test/rabbit/sensor-created
```

`rabbitmq/test/` 패키지에 있으며, 자세한 사용법은 해당 패키지의 `readme.md`와
프로젝트 루트의 `http/req.http` 파일을 참고하세요.
이 경로는 `/api/v1/**`이 아니므로 Swagger 문서에 나타나지 않습니다.
