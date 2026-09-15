# 07. 외부 연동

다른 서비스·인프라와 맞닿는 지점을 모았습니다.
비동기 메시지는 [05. 메시징](05-messaging.md)에 따로 있고, 여기서는 **동기 호출과 스토리지**를 다룹니다.

## 통신 방식 선택 기준

| 상황 | 방식 | 예시 |
| --- | --- | --- |
| 지금 당장 값이 있어야 응답할 수 있음 | **Feign (동기)** | 멤버 목록에 닉네임 붙이기 |
| 알려주기만 하면 되고 결과를 기다릴 필요 없음 | **RabbitMQ (비동기)** | 수확 완료 알림 |
| 상대가 값을 요청해 오고, 응답을 돌려줘야 함 | **RabbitMQ RPC** | AI 서버의 환경 준수율 요청 |

---

## Feign 클라이언트

`cultivation/client/` 패키지에 두 개가 있습니다.

### UserClient — 사용자 정보 조회

```java
@FeignClient(name = "user-server", url = "${feign.client.user-server.url}")
public interface UserClient {
    @GetMapping("/api/v1/users/batch")
    List<UserSummaryResponse> getUsers(@RequestParam("ids") List<Long> ids);
}
```

| 항목 | 내용 |
| --- | --- |
| 대상 | User Server |
| 설정 | `USER_SERVER_URL` |
| 사용처 | `CultivationMemberServiceImpl.getMembers()` |

이 서비스는 `cultivation_member` 테이블에 **`userId`만** 저장합니다.
닉네임 같은 사용자 정보는 User Server의 소유이므로 복제하지 않고, 필요할 때 조회해서 합칩니다.

멤버가 10명이면 10번 호출하는 대신 **ID 목록을 한 번에 넘기는 batch API**를 씁니다(N+1 방지).

```java
Map<Long, String> nicknameByUserId = userClient.getUsers(
        members.stream().map(CultivationMember::getUserId).toList()
).stream().collect(Collectors.toMap(UserSummaryResponse::userId, UserSummaryResponse::nickname));
```

> User Server가 응답하지 않으면 멤버 목록 조회도 실패합니다.
> 닉네임 없이라도 목록을 보여줘야 한다면 폴백 처리를 검토하세요.

### AiClient — 버섯 재배 가이드 조회

```java
@FeignClient(name = "ai-server", url = "${feign.client.ai-server.url}")
public interface AiClient {
    @GetMapping("/api/v1/mushrooms/{mushroom-id}/guide")
    ApiResponse<MushGuideResponse> getMushroomGuide(@PathVariable("mushroom-id") Long mushroomId);
}
```

| 항목 | 내용 |
| --- | --- |
| 대상 | AI Server |
| 설정 | `AI_SERVER_URL` |
| 사용처 | `MushGuideServiceImpl` — `GET /api/v1/mushrooms/{id}/guide` 요청을 그대로 위임 |

AI 서버 응답은 `ApiResponse<T>` 봉투에 담겨 오므로 `.data()`를 꺼내 반환합니다.

---

## 이 서비스를 호출하는 쪽

다른 서비스가 Cultivation Server의 **내부 API**를 호출하는 경우입니다.

| 호출자 | 엔드포인트 | 목적 |
| --- | --- | --- |
| AI Server | `GET /api/v1/internal/cultivations/photos/daily?date=` | 일일 리포트용 사진 수집 |
| AI Server | `PUT /api/v1/internal/cultivations/{id}/harvest/product-score` | 이미지 판정 결과를 품질 점수로 반영 |
| Data Generator | `GET /api/v1/internal/data-generator/snapshot` | 재기동 시 센서·임계값 상태 동기화 |

내부 API는 멤버십 검증을 하지 않습니다(`updateProductScoreInternal`은 권한 검사 없이 동작).
**클러스터 외부에 노출되지 않는다는 전제**가 필요합니다.

---

## MinIO 사진 흐름

생육 사진 원본은 MinIO에, 참조 키(`objectKey`)만 PostgreSQL에 둡니다.

### 업로드

```
POST /api/v1/cultivations/{id}/photos   (multipart/form-data, 최대 8MB)
```

1. `CultivationAccessGuard.requireMember()` — 멤버십 확인
2. `ObjectKeyGenerator.generate(DOMAIN, cultivationId, originalFilename)` — 오브젝트 키 생성
3. MinIO에 업로드
4. `CultivationPhoto` 레코드 저장
5. **DB 저장이 실패하면 MinIO 객체를 보상 삭제**

4번과 5번 사이가 이 흐름의 핵심입니다.
MinIO는 트랜잭션에 참여하지 않으므로, DB 롤백 시 객체만 남는 고아 상태가 될 수 있습니다.
이를 막기 위해 실패 시 `removeQuietly()`로 정리합니다.

```java
try {
    minioObjectStorage.put(objectKey, file);
    // ... DB 저장
} catch (Exception e) {
    minioObjectStorage.removeQuietly(objectKey);   // 보상 삭제
    throw ...;
}
```

삭제 시에도 `TransactionSynchronization.afterCommit()` 시점에 MinIO 객체를 지워,
DB 트랜잭션이 롤백되면 파일이 남아 있도록 합니다.

### 조회 — presigned URL과 프록시 치환

MinIO 버킷을 공개하지 않고, 조회할 때마다 **유효 기간이 있는 presigned URL**을 발급합니다.

| 상수 | 값 | 의미 |
| --- | --- | --- |
| `PRESIGNED_URL_TTL` | 30분 | URL 자체의 유효 기간 |
| `PRESIGNED_URL_CACHE_TTL` | 25분 | Redis 캐시 보관 기간 |
| `PRESIGNED_URL_CACHE_PREFIX` | `cultivation:photo:presigned-url:` | 캐시 키 접두사 |

캐시 TTL(25분)을 URL TTL(30분)보다 **짧게** 둔 것이 중요합니다.
캐시에서 꺼낸 URL이 곧바로 만료되는 상황을 5분의 여유로 방지합니다.

발급된 URL은 MinIO의 내부 주소를 가리키므로, 외부에서 접근할 수 있도록 베이스 URL을 치환합니다.

```java
String publicUrl = presignedUrl.startsWith(minioInternalBaseUrl)
        ? minioPublicBaseUrl + presignedUrl.substring(minioInternalBaseUrl.length())
        : presignedUrl;
```

```
MinIO 내부 주소        http://minio.internal:9000/bucket/key?X-Amz-...
        ↓ 치환
공개 프록시 주소        https://yes-nhn.site/storage-proxy/bucket/key?X-Amz-...
```

| 환경 변수 | 설명 |
| --- | --- |
| `MINIO_URL` | MinIO 내부 접속 주소 (SDK가 사용) |
| `MINIO_PUBLIC_BASE_URL` | 외부에 노출할 프록시 주소 (예: `https://yes-nhn.site/storage-proxy`) |
| `MINIO_ACCESS_KEY` · `MINIO_SECRET_KEY` | 인증 정보 |
| `MINIO_BUCKET` | 버킷 이름 |

> 서명(`X-Amz-Signature`)은 경로와 호스트를 포함해 계산되므로,
> 프록시는 경로를 **그대로 전달**해야 합니다. 경로를 재작성하면 서명 검증이 깨집니다.

### 삭제

```
DELETE /api/v1/cultivations/{id}/photos/{photo-id}
```

DB 레코드를 지우고, 커밋 이후 MinIO 객체를 제거합니다.

---

## 연동 지점 요약

```
                    ┌─────────────────────────┐
   User Server ◀────┤  Feign: 닉네임 일괄 조회 │
                    │                         │
   AI Server   ◀────┤  Feign: 재배 가이드 조회 │
                    │                         │
                    │   Cultivation Server    │
   AI Server   ────▶│  내부 API: 사진·점수     │
   Data Gen.   ────▶│  내부 API: 스냅샷        │
                    │                         │
   MinIO       ◀───▶│  사진 업로드 · presigned │
   InfluxDB    ◀────┤  센서 시계열 조회        │
   Redis       ◀───▶│  캐시 · 분산 락          │
   PostgreSQL  ◀───▶│  트랜잭션 데이터         │
                    └─────────────────────────┘
```
