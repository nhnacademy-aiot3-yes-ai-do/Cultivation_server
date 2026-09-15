# 02. 도메인 모델

## 전체 관계도

```
                      MushroomReference (버섯 종)
                       │              │
                       │              └──< MushroomReferenceThreshold >── SensorType
                       │                     (종별 생육/수확 표준값)         │
                       ▼                                                    │
   ┌──────────── Cultivation (재배지) ────────────┐                          │
   │                   │                          │                          │
   │                   ├──< CultivationMember     │  (OWNER/MANAGER/MEMBER)  │
   │                   ├──< CultivationPhoto      │  (MinIO objectKey)       │
   │                   └──1 Harvest               │  (재배당 1건)            │
   │                                              │                          │
   │   cultivationId 로 느슨하게 연결 ↓            │                          │
   ├── CultivationSensor ──< CultivationSensorType >──────────────────────────┤
   │      (물리 센서)            (센서-타입 매핑)                             │
   └── EnvironmentSetting ────────────────────────────────────────────────────┘
          (재배지별 실제 적용 임계값)
```

`CultivationSensor`와 `EnvironmentSetting`은 `Cultivation`을 FK 연관이 아니라
`cultivationId` 컬럼으로 참조합니다. 센서 도메인이 재배 도메인과 느슨하게 유지되도록 한 선택입니다.

---

## 엔티티

### Cultivation — 재배지

재배의 중심 엔티티입니다. 하나의 재배지는 하나의 버섯 종을 기릅니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `userId` | Long | 현재 소유자의 사용자 ID. 소유권 위임 시 갱신됨 |
| `name` | String(100) | 재배지 이름 |
| `mode` | `CultivationMode` | `GROWTH`(생육) / `HARVEST`(수확). 기본값 `GROWTH` |
| `cultivationStatus` | `CultivationStatus` | `CREATED` / `RUNNING` / `FINISHED` / `DELETED`. 기본값 `CREATED` |
| `startedAt` | LocalDateTime | 재배 시작 시각 |
| `finishedAt` | LocalDateTime | 재배 종료 시각 |
| `deletedAt` | LocalDateTime | 소프트 삭제 시각. `null`이면 살아있음 |
| `createdAt` / `updatedAt` | LocalDateTime | 생성 · 수정 시각 |
| `mushroomReference` | FK → MushroomReference | 재배 중인 버섯 종 |

### CultivationMember — 재배지 멤버

재배지에 참여하는 사용자와 그 역할입니다. 권한 규칙은 [04. 인증과 권한](04-authorization.md)에 있습니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `userId` | Long | 사용자 ID (User Server 소유) |
| `role` | `MemberRole` | `OWNER` / `MANAGER` / `MEMBER` |
| `joinedAt` | LocalDateTime | 참여 시각 |
| `cultivation` | FK → Cultivation | 소속 재배지 |

### CultivationPhoto — 생육 사진

원본 이미지는 MinIO에, 참조 키만 DB에 둡니다. 업로드·조회 흐름은
[07. 외부 연동](07-external-integration.md#minio-사진-흐름)을 참고하세요.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `objectKey` | String(500) | MinIO 오브젝트 키 |
| `storageType` | `StorageType` | 현재는 `MINIO` 고정 |
| `uploadedAt` | LocalDateTime | 업로드 시각 |
| `createdAt` | LocalDateTime | 레코드 생성 시각 |
| `cultivation` | FK → Cultivation | 소속 재배지 |

### Harvest — 수확 기록

`cultivation_id`에 unique 제약이 있어 **재배지당 수확 기록은 1건**입니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `harvestWeight` | BigDecimal | 수확량 |
| `memo` | TEXT | 메모 |
| `harvestedAt` | LocalDateTime | 수확 시각 |
| `productScore` | BigDecimal | 상위 백분위 점수. AI 서버가 내부 API로 갱신 |
| `productGrade` | `ProductGrade` | 점수에서 파생된 등급 |
| `cultivation` | FK → Cultivation (unique) | 소속 재배지 |

### MushroomReference — 버섯 종 레퍼런스

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `mushroomNameKo` / `mushroomNameEn` | String | 한글명 · 영문명 |
| `mushroomScientificName` | String (unique) | 학명 |
| `createdAt` / `updatedAt` | LocalDateTime | 생성 · 수정 시각 |

### MushroomReferenceThreshold — 종별 표준 환경값

"이 버섯은 생육기에 온도 몇 도, 수확기에 몇 도가 적정인가"를 담습니다.
재배지를 만들 때의 초기값과 수확 모드 전환 시의 재적용값이 여기서 나옵니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `sensorType` | FK → SensorType | 대상 센서 타입 |
| `mushroomReference` | FK → MushroomReference | 대상 버섯 종 |
| `thresholdType` | `MushroomReferenceThresholdType` | `GROWTH` / `HARVEST` |
| `thresholdMin` / `thresholdMax` | BigDecimal(10,4) | 임계값 범위 |

> unique 제약: (`sensor_type_id`, `mushroom_reference_id`, `threshold_type`)

### SensorType — 센서 타입

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `type` | String | 타입 이름 (예: 온도, 습도, CO₂, 조도) |
| `valueUnit` | String | 단위 (예: `°C`, `%`, `ppm`, `lux`) |

> unique 제약: (`type`, `value_unit`) — 같은 타입이라도 단위가 다르면 별개로 봅니다.
> 온도의 섭씨/화씨가 이 구조로 구분됩니다.

### CultivationSensor — 재배지에 설치된 물리 센서

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `cultivationId` | long | 소속 재배지 ID (FK 연관 아님) |
| `deviceEui` | String | 장치 고유 식별자 |
| `deviceModel` / `deviceName` | String | 모델명 · 표시 이름 |
| `location` / `locationDetail` | String | 설치 위치 · 상세 위치 |
| `sensorStatus` | `SensorConnectStatus` | `ONLINE` / `OFFLINE` / `ERROR` |
| `monitoringStartedAt` | Instant | 모니터링 시작 시각 |
| `lastMeasuredAt` | Instant | 마지막 측정 시각. 연결 상태 판정에 사용 |
| `createdAt` | LocalDateTime | 생성 시각 |
| `isDeleted` | boolean | 소프트 삭제 여부. 재등록 시 복구됨 |

> unique 제약: (`cultivation_id`, `device_eui`) — 같은 재배지에 같은 장치를 중복 등록할 수 없습니다.

### CultivationSensorType — 센서와 타입의 매핑

하나의 물리 센서가 여러 값을 측정할 수 있으므로(예: 온습도 센서) 다대다 관계를 풀어낸 연결 테이블입니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `cultivationSensor` | FK → CultivationSensor | 물리 센서 |
| `sensorType` | FK → SensorType | 측정 타입 |

> unique 제약: (`cultivation_sensor_id`, `sensor_type_id`)

### EnvironmentSetting — 재배지별 적용 임계값

`MushroomReferenceThreshold`가 "표준값"이라면, 이쪽은 **해당 재배지에 실제로 적용 중인 값**입니다.
사용자가 직접 수정할 수 있고, 이 값이 Rule Engine으로 발행되어 판정 기준이 됩니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | PK |
| `cultivationId` | long | 소속 재배지 ID (FK 연관 아님) |
| `sensorType` | FK → SensorType | 대상 센서 타입 |
| `thresholdMin` / `thresholdMax` | BigDecimal(10,4) | 적용 임계값 범위 |

> unique 제약: (`cultivation_id`, `sensor_type_id`)

---

## Enum

### CultivationStatus — 재배 상태

```
CREATED ──▶ RUNNING ──▶ FINISHED
   │           │            │
   └───────────┴────────────┴──▶ DELETED (소프트 삭제)
```

| 값 | 의미 |
| --- | --- |
| `CREATED` | 생성 직후. 아직 시작 전 |
| `RUNNING` | 재배 진행 중 |
| `FINISHED` | 종료됨. 이력 조회 대상 |
| `DELETED` | 삭제됨. `deletedAt`이 채워짐 |

### CultivationMode — 재배 모드

| 값 | 의미 |
| --- | --- |
| `GROWTH` | 생육기. 버섯 레퍼런스의 `GROWTH` 임계값이 적용됨 |
| `HARVEST` | 수확기. 전환 시 `HARVEST` 임계값으로 자동 재적용됨 |

전환은 단방향입니다. 이미 `HARVEST`인 재배지에 다시 전환을 요청하면
`CultivationAlreadyInHarvestModeException`이 발생합니다.

```
GROWTH ──[PUT /harvest-mode]──▶ HARVEST
```

전환 시 일어나는 일은 [수확 모드 전환](#수확-모드-전환-상세)에 정리했습니다.

### MemberRole — 멤버 역할

| 값 | 의미 |
| --- | --- |
| `OWNER` | 소유자. 재배지당 1명. 멤버 관리와 소유권 위임 가능 |
| `MANAGER` | 관리자. 재배 운영은 가능하나 멤버 관리는 불가 |
| `MEMBER` | 일반 멤버. 조회 중심 |

### ProductGrade — 수확물 품질 등급

`productScore`(상위 백분위, 작을수록 우수)에서 `fromPercentile()`로 파생됩니다.

| 등급 | 조건 (상위 백분위) |
| --- | --- |
| `TOP` | ≤ 0.05 (상위 5% 이내) |
| `HIGH` | ≤ 0.20 (상위 20% 이내) |
| `MID` | ≤ 0.50 (상위 50% 이내) |
| `LOW` | 그 외 |

### MushroomReferenceThresholdType

| 값 | 의미 |
| --- | --- |
| `GROWTH` | 생육기 표준 환경값 |
| `HARVEST` | 수확기 표준 환경값 |

### SensorConnectStatus — 센서 연결 상태

| 값 | 의미 |
| --- | --- |
| `ONLINE` | 정상 수신 중 |
| `OFFLINE` | 일정 시간 데이터 없음 |
| `ERROR` | 오류 상태 |

### LatestSensorCacheStatus — 최신값 캐시 상태

최신 센서값 조회 응답에 함께 실려, **값이 얼마나 믿을 만한지**를 알려줍니다.
자세한 내용은 [06. 센서 파이프라인](06-sensor-pipeline.md)에 있습니다.

| 값 | 의미 |
| --- | --- |
| `FRESH` | 캐시가 최신 상태 |
| `PARTIAL` | 일부 센서 타입만 최신 |
| `SOURCE_FALLBACK` | 캐시를 못 써서 InfluxDB에서 직접 조회 |
| `NO_DATA` | 데이터 없음 |
| `REDIS_PENDING` | 캐시 준비 중(워밍업) |

### StorageType

| 값 | 의미 |
| --- | --- |
| `MINIO` | MinIO 오브젝트 스토리지 (현재 유일) |

---

## 수확 모드 전환 상세

`PUT /api/v1/cultivations/{id}/harvest-mode` 호출 시 `CultivationModeFacade`가 다음 순서로 처리합니다.

1. `CultivationService.switchToHarvestMode()` — 재배 상태를 `HARVEST`로 변경
2. 재배지의 버섯 종(`mushroomReference`) 확인
3. 해당 재배지에 **실제로 등록된 센서 타입** 목록 조회
4. 버섯 종의 `HARVEST` 표준 임계값 중, 3번에서 구한 센서 타입에 해당하는 것만 필터
5. 필터된 임계값으로 `EnvironmentSetting` 재적용
6. `ThresholdInfoEvent` 발행 → Rule Engine · Data Generator가 판정 기준 갱신

4번의 필터링이 중요합니다. 버섯 레퍼런스에 CO₂ 표준값이 있어도
그 재배지에 CO₂ 센서가 없으면 임계값을 만들지 않습니다.
등록된 센서가 하나도 없으면 빈 목록이 적용되고, Rule Engine 쪽 임계값도 모두 삭제됩니다.

---

## 데이터베이스 마이그레이션

스키마는 Flyway로 관리하며 `src/main/resources/db/migration/`에 있습니다.

| 버전 | 파일 | 내용 |
| --- | --- | --- |
| V1 | `V1__initial_cultivation_schema.sql` | 초기 스키마 |

설정상 `baseline-on-migrate: true`, `baseline-version: 0`이며,
스키마는 `CULTIVATION_DB_SCHEMA` 환경 변수로 지정합니다.

> JPA의 `ddl-auto`가 `update`로 설정되어 있어 Flyway와 함께 동작합니다.
> 운영 환경에서 스키마 변경은 반드시 Flyway 마이그레이션 파일로 관리하세요.
