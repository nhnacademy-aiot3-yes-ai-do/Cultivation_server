# 03. API 명세

요청·응답 본문의 상세 스펙은 **Swagger UI(`/swagger-ui.html`)**에 있습니다.
이 문서는 "어떤 API가 있고, 누가 호출할 수 있고, 무엇을 하는가"를 한눈에 보기 위한 목록입니다.

## 공통 규약

| 항목 | 내용 |
| --- | --- |
| Base URL | 운영 `https://api.yes-nhn.site` · 로컬 Gateway `http://localhost:8000` |
| 인증 헤더 | `X-User-Id` (Gateway가 JWT 검증 후 주입) |
| 역할 헤더 | `X-User-Role` (선택. `ADMIN`이면 멤버십 검사 우회) |
| 오류 응답 | RFC 7807 `application/problem+json` |
| 경로 변수 표기 | 케밥 케이스 (`{cultivation-id}`) |

> 서비스를 Gateway 없이 직접 호출할 때는 `X-User-Id` 헤더를 직접 넣어야 합니다.

권한 열의 표기는 다음과 같습니다.

| 표기 | 의미 |
| --- | --- |
| 멤버 | 해당 재배지의 멤버 누구나 (`ADMIN`은 우회 가능) |
| MANAGER↑ | `MANAGER` 또는 `OWNER` |
| OWNER | 소유자만 |
| 인증 | 로그인만 되어 있으면 됨 (재배지 멤버십 무관) |
| 내부 | 서비스 간 호출 전용 (Swagger 미노출) |

---

## 재배 (Cultivation)

`/api/v1/cultivations`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `POST` | `/` | 재배 생성. 요청자를 `OWNER`로 등록하고 초기 환경 설정을 적용한 뒤 임계값 이벤트 발행 | 인증 |
| `GET` | `/` | 내가 멤버로 속한 **진행 중** 재배 목록 | 인증 |
| `GET` | `/{cultivation-id}` | 재배 상세 조회 | 멤버 |
| `PUT` | `/{cultivation-id}/finish` | 재배 종료. 임계값을 빈 목록으로 발행해 판정 중단 | OWNER |
| `GET` | `/history` | 종료된 재배 이력 (페이지네이션) | 인증 |
| `DELETE` | `/{cultivation-id}` | 재배 소프트 삭제 (`deletedAt` 기록) | OWNER |
| `PUT` | `/{cultivation-id}/harvest-mode` | 수확 모드로 전환하고 수확용 임계값 재적용 | MANAGER↑ |

`GET /history`는 `Pageable`(`page` · `size` · `sort`)을 받습니다.

## 재배 메타데이터

`/api/v1/cultivations`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `GET` | `/metadata` | 재배 생성 화면에 필요한 메타데이터(버섯 종·센서 타입 등) | 인증 |
| `GET` | `/{cultivation-id}/metadata` | 특정 재배지의 메타데이터 | 멤버 |

## 멤버 (Cultivation Member)

`/api/v1/cultivations/{cultivation-id}`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `POST` | `/members` | 멤버 추가. 추가된 사용자에게 알림 이벤트 발행 | OWNER |
| `GET` | `/members` | 멤버 목록. User Server에서 닉네임을 일괄 조회해 합침 | 멤버 |
| `PUT` | `/members/{user-id}` | 멤버 역할 변경. `OWNER`로는 변경 불가 | OWNER |
| `PUT` | `/owner` | 소유권 위임. 기존 소유자는 `MANAGER`로 강등 | OWNER |
| `DELETE` | `/members/{user-id}` | 멤버 삭제 | OWNER |

## 사진 (Cultivation Photo)

`/api/v1/cultivations/{cultivation-id}/photos`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `POST` | `/` | 사진 업로드 (`multipart/form-data`, 최대 8MB) | 멤버 |
| `GET` | `/` | 사진 목록. presigned URL을 함께 반환 | 멤버 |
| `DELETE` | `/{photo-id}` | 사진 삭제. MinIO 객체도 함께 제거 | 멤버 |

## 수확 (Harvest)

`/api/v1/cultivations/{cultivation-id}/harvest`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `POST` | `/` | 수확 등록. 재배지당 1건. 알림·AI 이벤트 발행 | MANAGER↑ |
| `GET` | `/` | 수확 기록 조회 | 멤버 |
| `PUT` | `/product-score` | 품질 점수 갱신 및 등급 재산정 | MANAGER↑ |

## 버섯 가이드

`/api/v1/mushrooms`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `GET` | `/{mushroom-id}/guide` | 재배 가이드 조회. AI Server로 Feign 위임 | 인증 |

---

## 센서 (Cultivation Sensor)

`/api/v1/cultivations/{cultivation-id}/sensors`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `POST` | `/` | 센서 등록. 소프트 삭제된 동일 `deviceEui`가 있으면 복구. 임계값·센서정보 이벤트 발행 | MANAGER↑ |
| `GET` | `/` | 재배지에 등록된 센서 목록 | 멤버 |
| `DELETE` | `/{sensor-id}` | 센서 삭제(소프트) 및 삭제 이벤트 발행 | MANAGER↑ |

`/api/v1/sensors`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `GET` | `/reusable` | 사용자가 과거에 쓴 센서 중 재사용 가능한 목록 | 인증 |

## 센서 값 (Sensor Value)

`/api/v1/cultivations/{cultivation-id}/sensor-values`

| 메서드 | 경로 | 쿼리 파라미터 | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| `GET` | `/` | — | 재배지의 최신 센서값. 캐시 상태(`LatestSensorCacheStatus`) 포함 | 멤버 |
| `GET` | `/trend` | `device-eui`, `sensor-type`, `unit` | 특정 센서의 시계열 추이 | 멤버 |
| `GET` | `/average` | — | 센서 타입별 평균값 | 멤버 |

`/api/v1/cultivations/sensor-values`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `GET` | `/latest` | 내가 속한 **모든** 재배지의 최신값을 한 번에 조회 | 인증 |

## 환경 설정 (Environment Setting)

`/api/v1/cultivations/{cultivation-id}/environment-settings`

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `PUT` | `/` | 임계값 수정. 섭씨·화씨를 함께 담은 임계값 이벤트 발행 | MANAGER↑ |

## 환경 준수율 (Environment Compliance)

`/api/v1/cultivations/{cultivation-id}/environment-compliance`

| 메서드 | 경로 | 쿼리 파라미터 | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| `GET` | `/` | — | 재배 전체 기간 준수율 | 멤버 |
| `GET` | `/daily` | `date` (선택, `yyyy-MM-dd`) | 특정 일자 준수율 | 멤버 |
| `GET` | `/period` | `startDate`, `endDate` (필수, `yyyy-MM-dd`) | 기간 준수율 | 멤버 |

응답은 온도·습도·CO₂·조도 4개 항목의 준수율입니다.

## 레퍼런스 조회

| 메서드 | 경로 | 설명 | 권한 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/mushroom-references` | 버섯 종 목록과 표준 환경값 | 인증 |
| `GET` | `/api/v1/sensor-types` | 지원 센서 타입 목록 | 인증 |

---

## 관리자 API

`/api/v1/admin/mushroom-references`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/` | 버섯 종 등록 |
| `GET` | `/` | 버섯 종 목록 |
| `GET` | `/{mushroom-reference-id}` | 버섯 종 단건 조회 |
| `PUT` | `/{mushroom-reference-id}` | 버섯 종 수정 |
| `DELETE` | `/{mushroom-reference-id}` | 버섯 종 삭제 |

`/api/v1/admin/sensor-types`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/` | 센서 타입 등록 |
| `GET` | `/{sensor-type-id}` | 센서 타입 단건 조회 |
| `PUT` | `/{sensor-type-id}` | 센서 타입 수정 |
| `DELETE` | `/{sensor-type-id}` | 센서 타입 삭제 |

> 관리자 API는 Gateway 라우팅 단계에서 `ADMIN` 역할을 요구하는 것을 전제로 합니다.

---

## 내부 API

`/api/v1/internal/**` 경로는 `springdoc.paths-to-exclude` 설정으로 **Swagger 문서에서 제외**됩니다.
외부에 노출하지 말고 서비스 간 호출에만 사용하세요.

| 메서드 | 경로 | 호출자 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/internal/cultivations/photos/daily` | AI Server | 일자별 사진 목록 수집 (`date` 파라미터) |
| `PUT` | `/api/v1/internal/cultivations/{cultivation-id}/harvest/product-score` | AI Server | 품질 점수 갱신 및 등급 재산정 |
| `GET` | `/api/v1/internal/data-generator/snapshot` | Data Generator | 센서·임계값 스냅샷 조회 (재기동 시 동기화용) |

## 테스트용 엔드포인트

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/internal/test/rabbit/sensor-created` | RabbitMQ 발행 스모크 테스트 |

`/api/v1/**` 패턴에 해당하지 않아 Swagger에 나타나지 않습니다. 운영 환경에서는 차단하는 것이 좋습니다.

---

## 알려진 경로 불일치

`InfluxDBSensorController`의 매핑만 버전 세그먼트가 빠져 있습니다.

```
POST /api/cultivations/{cultivation-id}/sensor-influx/average
     ^^^^ /api/v1 이 아님
```

`springdoc.paths-to-match`가 `/api/v1/**`이므로 **이 엔드포인트는 Swagger 문서에 나오지 않습니다.**
Gateway 라우팅 규칙도 `/api/v1/**` 기준이라면 외부에서 접근되지 않을 수 있으니,
사용 중인 경로인지 확인 후 `/api/v1/...`로 정렬하는 것을 권장합니다.
