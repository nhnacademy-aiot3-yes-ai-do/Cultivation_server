# 06. 센서 파이프라인

센서 측정값은 이 서비스가 **생산하지 않습니다.** Data Generator가 MQTT로 발행하고
Rule Engine이 InfluxDB에 적재한 값을, 이 서비스는 **읽어서 사용자에게 보여주는 쪽**입니다.

```
Data Generator ──MQTT──▶ Rule Engine ──▶ InfluxDB
                                            │
                                            │ 조회
                                            ▼
                              Cultivation Server ──▶ Redis 캐시 ──▶ 사용자 응답
```

문제는 **InfluxDB를 매 요청마다 조회하면 느리다는 것**입니다.
대시보드는 여러 재배지의 최신값을 자주 갱신하므로, 그때마다 시계열 질의를 날리면 부하가 큽니다.
그래서 스케줄러가 주기적으로 InfluxDB를 읽어 Redis에 캐시해두고, 조회 요청은 Redis에서 처리합니다.

---

## 캐시 스케줄러

`SensorCacheScheduler`가 주기적으로 InfluxDB를 폴링해 Redis를 갱신합니다.

```java
@Scheduled(fixedDelayString = "${sensor-cache.poll-interval-ms:2000}", ...)
public void poll() { ... }
```

### 인스턴스가 여러 개일 때

서비스를 여러 파드로 띄우면 모든 파드가 같은 재배지를 동시에 갱신하려 듭니다.
이를 막기 위해 **재배지 단위 분산 락**을 씁니다.

| 항목 | 내용 |
| --- | --- |
| 락 키 | `cultivation:sensor:cache:refresh-lock:{cultivationId}` |
| 획득 | `SETNX` + TTL(`lock-lease-seconds`, 기본 600초) |
| 갱신 | 하트비트 스레드가 `renew.lua`로 주기적 연장 |
| 해제 | `unlock.lua`로 **자기 토큰일 때만** 삭제 |

락을 획득한 인스턴스만 해당 재배지를 갱신합니다.
작업 도중 락을 잃으면(`LOCK_LOST`) 워터마크를 전진시키지 않고 중단해, 다른 인스턴스와 충돌하지 않습니다.

> `unlock.lua`와 `renew.lua`가 Lua인 이유는 "토큰 확인"과 "삭제·연장"이
> **원자적으로** 일어나야 하기 때문입니다. 두 명령으로 나누면 그 사이에 락이 만료될 수 있습니다.

### 워터마크 기반 증분 조회

매번 전체 구간을 조회하면 낭비이므로, 재배지별로 **어디까지 읽었는지**를 Redis에 기록합니다.

| 항목 | 내용 |
| --- | --- |
| 워터마크 키 | `cultivation:sensor:cache:watermark:{cultivationId}` |
| 조회 구간 | 마지막 워터마크 ~ 현재. 단 `query-overlap-seconds`(기본 60초)만큼 겹쳐서 조회 |

겹쳐 읽는 이유는 InfluxDB에 늦게 도착하는 데이터를 놓치지 않기 위해서입니다.
중복 데이터는 Redis Sorted Set의 score(측정 시각)로 자연스럽게 덮어써집니다.

### 워밍업과 정합성 보정

| 동작 | 시점 | 목적 |
| --- | --- | --- |
| `warmUp()` | 기동 직후 | 캐시가 빈 상태로 요청을 받지 않도록 초기 적재 |
| 정합성 보정 | `reconciliation-interval-seconds`(기본 300초)마다 | 누락분 보정 |

---

## Redis 캐시 구조

| 키 | 자료구조 | 내용 |
| --- | --- | --- |
| `raw:{cultivationId}:{deviceEui}:{sensorType}:{unit}` | Sorted Set | 측정 시각(score) → 타임스탬프(member) |
| 위 키 + `:values` | Hash | 타임스탬프 → 측정값 |
| `latest:{cultivationId}` | Hash | 센서별 최신값 (`update-latest.lua`로 원자적 갱신) |

이력은 `history-hours`(기본 12시간)만큼만 보관하며, TTL은 `history + ttl-grace`로 설정합니다.

### 다운샘플링(compaction)

12시간 치 원본을 그대로 들고 있으면 메모리가 커집니다.
오래된 데이터일수록 해상도를 낮춰 압축합니다(`resolutionForAge()`).
압축은 `rename-compaction.lua`로 원자적으로 교체되므로, 압축 도중 조회해도 깨진 데이터를 보지 않습니다.

---

## 최신값 조회와 캐시 상태

최신값 응답에는 `LatestSensorCacheStatus`가 함께 담겨 **값의 신뢰도**를 알려줍니다.

| 상태 | 의미 | 프론트엔드 권장 처리 |
| --- | --- | --- |
| `FRESH` | `freshness-seconds`(기본 9초) 이내의 최신값 | 그대로 표시 |
| `PARTIAL` | 일부 센서 타입만 최신 | 표시하되 일부 지연 안내 |
| `SOURCE_FALLBACK` | 캐시 미스로 InfluxDB 직접 조회 | 그대로 표시 (약간 느림) |
| `REDIS_PENDING` | 캐시 워밍업 중 | 로딩 표시 |
| `NO_DATA` | 데이터 없음 | "측정값 없음" 표시 |

캐시를 쓸 수 없을 때는 `fallback-wait-seconds`(기본 25초)만큼 기다린 뒤 InfluxDB로 직접 조회합니다.

### 관련 엔드포인트

| 경로 | 설명 |
| --- | --- |
| `GET /api/v1/cultivations/{id}/sensor-values` | 한 재배지의 최신값 |
| `GET /api/v1/cultivations/sensor-values/latest` | 내가 속한 **모든** 재배지의 최신값 (대시보드용) |
| `GET /api/v1/cultivations/{id}/sensor-values/trend` | 특정 센서의 추이 (`device-eui` · `sensor-type` · `unit` 필요) |
| `GET /api/v1/cultivations/{id}/sensor-values/average` | 센서 타입별 평균 |

---

## 센서 연결 상태

`SensorConnectionService`가 조회 결과의 `lastMeasuredAt`을 보고
`CultivationSensor.sensorStatus`를 `ONLINE` / `OFFLINE` / `ERROR`로 갱신합니다.

데이터가 끊긴 것으로 판단되면 `SensorDataUnavailableEvent`를 발행합니다.
**알림을 보낼지 말지는 이 서비스가 정하지 않고** Notification Server가 판단합니다
([05. 메시징](05-messaging.md) 참고).

> 상태 갱신 중 락을 잃으면 워터마크를 전진시키지 않습니다.
> 갱신이 절반만 반영된 채 "읽은 것으로 처리"되는 상황을 막기 위해서입니다.

---

## 환경 준수율

"설정한 임계값 범위 안에 실제 측정값이 얼마나 머물렀는가"를 백분율로 계산합니다.
AI 리포트의 핵심 지표이며, AI Server가 RabbitMQ RPC로도 요청합니다.

### 계산 방식

1. 재배지의 `EnvironmentSetting`(센서 타입별 min/max)을 조회
2. InfluxDB에서 해당 기간의 측정값을 가져와 **범위 안에 든 개수**와 **전체 개수**를 집계
3. 비율을 백분율로 환산

```java
private BigDecimal rate(long inRangeCount, long total) {
    return BigDecimal.valueOf(inRangeCount)
            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
}
```

결과는 소수점 둘째 자리까지의 **백분율(0~100)**입니다.

### 응답 형태

```java
public record EnvironmentComplianceResponse(
        BigDecimal temperatureCompliance,
        BigDecimal humidityCompliance,
        BigDecimal co2Compliance,
        BigDecimal lightCompliance
) {}
```

온도 · 습도 · CO₂ · 조도 네 항목 고정입니다. 센서 타입을 늘리려면 이 레코드도 함께 바꿔야 합니다.

### 화씨 처리

`SensorType`은 (`type`, `value_unit`) 조합으로 구분되므로 섭씨와 화씨가 별개 레코드입니다.
준수율 계산에서는 **화씨 설정을 건너뜁니다.** 같은 온도를 두 번 집계해
결과가 왜곡되는 것을 막기 위해서입니다. 화씨 값은 Rule Engine 쪽 판정을 위해
임계값 이벤트에는 섭씨와 함께 실려 나갑니다(`TemperatureThresholdConverter`).

### 조회 경로

| 방식 | 경로 | 용도 |
| --- | --- | --- |
| REST | `GET .../environment-compliance` | 전체 기간 |
| REST | `GET .../environment-compliance/daily?date=YYYY-MM-DD` | 특정 일자 |
| REST | `GET .../environment-compliance/period?startDate=&endDate=` | 기간 |
| RabbitMQ RPC | `yes-nhn.environment.compliance.queue` | AI Server의 리포트 생성 |

---

## 설정 값

`sensor-cache.*` 프로퍼티로 조정하며, 모두 환경 변수로 덮어쓸 수 있습니다.

| 프로퍼티 | 환경 변수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `history-hours` | `SENSOR_CACHE_HISTORY_HOURS` | `12` | 캐시에 보관할 이력 시간 |
| `poll-interval-ms` | `SENSOR_CACHE_POLL_INTERVAL_MS` | `2000` | 폴링 주기 |
| `freshness-seconds` | `SENSOR_CACHE_FRESHNESS_SECONDS` | `9` | `FRESH` 판정 기준 |
| `ttl-grace-seconds` | `SENSOR_CACHE_TTL_GRACE_SECONDS` | `3` | TTL 여유분 |
| `query-overlap-seconds` | `SENSOR_CACHE_QUERY_OVERLAP_SECONDS` | `60` | 중복 조회 구간 |
| `poll-initial-delay-ms` | `SENSOR_CACHE_POLL_INITIAL_DELAY_MS` | `10000` | 기동 후 첫 폴링 지연 |
| `lock-lease-seconds` | `SENSOR_CACHE_LOCK_LEASE_SECONDS` | `600` | 분산 락 TTL |
| `reconciliation-interval-seconds` | `SENSOR_CACHE_RECONCILIATION_INTERVAL_SECONDS` | `300` | 정합성 보정 주기 |
| `fallback-wait-seconds` | `SENSOR_CACHE_FALLBACK_WAIT_SECONDS` | `25` | 폴백 전 대기 시간 |

### 튜닝 가이드

| 증상 | 조정 |
| --- | --- |
| 값이 너무 늦게 갱신됨 | `poll-interval-ms` ↓ (InfluxDB 부하 증가 주의) |
| `PARTIAL`이 자주 뜸 | `freshness-seconds` ↑ |
| Redis 메모리 부족 | `history-hours` ↓ |
| 늦게 도착한 데이터 누락 | `query-overlap-seconds` ↑ |

---

## InfluxDB 접속

| 설정 | 환경 변수 |
| --- | --- |
| URL · 조직 · 토큰 | `INFLUX_URL` · `INFLUX_ORG` · `INFLUX_TOKEN` |
| 버킷 | `INFLUX_BUCKET` (기본 `sensor-data`) |
| Cloudflare Access | `CF_ACCESS_CLIENT_ID` · `CF_ACCESS_CLIENT_SECRET` |

InfluxDB 앞단에 Cloudflare Access가 있는 경우, 위 두 값이 요청 헤더로 자동 첨부됩니다.
로컬 InfluxDB를 쓸 때는 비워두면 됩니다.
