# 09. 트러블슈팅 회고

Cultivation Server는 김진영·김동건·이동희 세 명이 함께 개발합니다.
이 문서는 각자 실제로 부딪혔던 문제와 해결 과정을 **본인이 직접** 기록하는 공간입니다.

[08. 로컬 개발](08-local-development.md)의 트러블슈팅이 "환경을 어떻게 맞추는가"에 대한 것이라면,
이 문서는 "설계·코드 수준에서 무엇이 왜 잘못됐고 어떻게 고쳤는가"를 다룹니다.
커밋 해시를 함께 남겨서, 필요하면 `git show <hash>`로 실제 변경 내용을 바로 확인할 수 있게 했습니다.

> 작성 형식: 문제 상황 → 원인 → 해결 → 배운 점. 본인 파트를 추가할 때는 이 형식을 따라주세요.

---

## 김진영 — 사진(Photo) 로직

재배 사진은 MinIO에 저장하고, 참조 키만 PostgreSQL에 둡니다.
이 사진을 **프론트가 어떻게 받아 가게 할 것인가**를 두고 세 번 방식을 바꿨습니다.
바뀐 순서 자체가 문제를 하나씩 발견하고 고쳐나간 기록입니다.

### 1차 — 사진 URL을 열면 403이 남 (인증 문제)

**문제 상황**
사진 업로드에 성공하고 응답으로 받은 URL을 브라우저에서 그대로 열면 MinIO가 `AccessDenied`(403)를 반환했습니다.
업로드는 되는데 정작 그 사진을 볼 수가 없는 상태였습니다.

**원인**
당시 URL 생성 로직(`DefaultStorageUrlResolver`)은 서명 없이 문자열만 조합하고 있었습니다.

```java
// site.yesaido.common.storage.DefaultStorageUrlResolver
public String resolve(StorageType storageType, String objectKey) {
    return switch (storageType) {
        case MINIO -> "%s/%s/%s".formatted(minioBaseUrl, minioBucket, objectKey);
        case LOCAL -> "%s/%s".formatted(localBaseUrl, objectKey);
    };
}
```

이 방식은 버킷이 **공개 읽기(public-read)**로 열려 있을 때만 동작합니다.
사진에 재배 멤버만 접근해야 한다는 요구사항상 버킷을 비공개로 둘 수밖에 없었고,
그 결과 서명 없는 URL은 전부 거부당했습니다. "URL 형식은 맞는데 왜 안 열리지"로 한참 헤맸는데,
결국 버킷 정책과 URL 발급 방식이 서로 안 맞았던 것이 원인이었습니다.

**해결 (1차)**
사진 조회를 프론트가 MinIO에 직접 요청하는 대신, Cultivation Server를 한 번 거치도록 바꿨습니다.
서버가 재배 멤버십을 확인한 뒤에만 MinIO에서 바이트를 읽어 그대로 응답으로 흘려보내는 방식입니다.

```java
@GetMapping("/{photo-id}/raw")
public ResponseEntity<byte[]> getPhotoRaw(...) {
    PhotoRawContent raw = cultivationPhotoService.getPhotoRaw(cultivationId, userId, photoId);
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(raw.contentType()))
            .body(raw.bytes());
}
```

인증은 이제 MinIO 버킷 정책이 아니라 서비스 코드가 직접 책임지게 되어 문제 자체는 해결됐지만,
이 방식은 곧 아래 두 가지 문제를 새로 드러냈습니다.

### 2차 — MinIO 다운로드가 DB 트랜잭션을 물고 있음

**문제 상황**
`getPhotoRaw`가 속한 서비스 클래스는 클래스 레벨에 `@Transactional(readOnly = true)`가 걸려 있었습니다.
즉 "멤버십 확인 + DB 조회"뿐 아니라 그 뒤에 이어지는 **MinIO 네트워크 다운로드**까지 같은 트랜잭션 안에서 실행되고 있었습니다.
사진이 크거나 MinIO 응답이 느려지면 그동안 DB 커넥션 하나를 계속 붙들고 있는 셈이라,
동시 요청이 늘어나면 커넥션 풀 고갈로 이어질 수 있는 구조였습니다.

**원인**
"DB 트랜잭션 범위"와 "실제로 트랜잭션이 필요한 범위"가 다르다는 것을 처음엔 구분하지 않았습니다.
멤버십 확인·사진 존재 확인은 DB 트랜잭션이 필요하지만, 그 이후의 MinIO 다운로드는 트랜잭션과 아무 관계가 없는 순수 I/O입니다.

**해결**
DB 조회 부분(멤버십 확인 + `objectKey` 조회)을 `CultivationPhotoAccessValidator`라는 별도 컴포넌트로 분리해
자체적으로 짧은 `@Transactional(readOnly = true)`를 걸고 즉시 커밋되게 했습니다.
그리고 `getPhotoRaw` 자체에는 `Propagation.NOT_SUPPORTED`를 명시해서,
호출하는 쪽에 트랜잭션이 열려 있더라도 이 메서드만큼은 트랜잭션 밖에서 실행되도록 강제했습니다.

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public PhotoRawContent getPhotoRaw(Long cultivationId, Long userId, Long photoId) {
    String objectKey = cultivationPhotoAccessValidator.resolveObjectKey(cultivationId, userId, photoId);
    try (GetObjectResponse response = minioClient.getObject(
            GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
        byte[] bytes = response.readAllBytes();
        String contentType = response.headers().get("Content-Type");
        return new PhotoRawContent(bytes, contentType != null ? contentType : "application/octet-stream");
    } catch (Exception e) {
        throw new CustomServerException(...);
    }
}
```

**배운 점**
"이 메서드에 트랜잭션이 필요한가"가 아니라 "이 메서드 **전체**가 트랜잭션이어야 하는가"를 따져봐야 한다는 걸 배웠습니다.
느릴 수 있는 외부 I/O(네트워크 호출, 파일 다운로드 등)는 되도록 트랜잭션 바깥으로 빼는 습관을 이때부터 들였습니다.

### 3차 — 서버를 거치는 방식 자체의 한계 → Presigned URL로 전환

**문제 상황**
1차 해결로 인증 문제는 없앴지만, 사진 하나를 볼 때마다
`클라이언트 → Cultivation Server → MinIO → Cultivation Server → 클라이언트` 순으로 바이트가 두 번 오갔습니다.
사진을 여러 장 띄우는 화면(재배 상세, 갤러리)에서는 앱 서버가 이미지 전송 자체를 대신 떠맡는 구조라
트래픽이 늘수록 앱 서버 스레드와 대역폭을 불필요하게 잡아먹었습니다.

**원인**
"인증이 필요하다"는 요구사항을 "요청마다 서버를 거쳐야 한다"로 잘못 풀었던 것이 근본 원인이었습니다.
실제로 필요한 건 "권한 있는 사람에게만, 일정 시간 동안만 접근을 허용"하는 것이었지,
매번 바이트를 서버가 중계해야 하는 건 아니었습니다.

**해결**
MinIO SDK가 제공하는 **presigned URL**(서명 + 유효시간이 내장된 URL) 방식으로 바꿨습니다.
서버는 멤버십을 확인한 시점에 한 번만 서명된 URL을 발급하고, 그 이후의 실제 이미지 전송은
클라이언트가 MinIO에 직접 요청하므로 앱 서버를 거치지 않습니다.

```java
private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(30);

private PhotoUploadResponse toResponse(CultivationPhoto cultivationPhoto) {
    String objectKey = cultivationPhoto.getObjectKey();
    String presignedUrl = minioObjectStorage.presignedGetUrl(objectKey, PRESIGNED_URL_TTL);
    ...
}
```

`getPhotoRaw` 엔드포인트와 `CultivationPhotoAccessValidator`는 이 시점 이후로 더 이상 필요하지 않게 되어
현재 코드에는 남아 있지 않습니다(완전히 제거됨). 지금 조회 흐름의 최종 형태는
[07. 외부 연동 — MinIO 사진 흐름](07-external-integration.md#minio-사진-흐름)에 정리되어 있습니다.

### 4차 — 매 요청마다 서명하는 비용 → Redis 캐싱

**문제 상황**
사진 목록 조회, AI 서버의 일일 사진 조회처럼 같은 사진을 여러 번 조회하는 경로가 늘어나면서
조회할 때마다 MinIO에 새 서명을 요청하는 게 낭비로 느껴졌습니다.

**해결**
발급한 presigned URL을 Redis에 캐싱했습니다. 이때 **캐시 TTL(25분)을 URL 자체의 TTL(30분)보다 짧게** 잡은 것이
핵심입니다. 캐시에서 막 꺼낸 URL이 곧바로 만료되어 버리는 상황을 5분의 여유로 방지하기 위해서입니다.

```java
private static final Duration PRESIGNED_URL_CACHE_TTL = Duration.ofMinutes(25);
private static final String PRESIGNED_URL_CACHE_PREFIX = "cultivation:photo:presigned-url:";

private String presignedUrlCached(String objectKey) {
    String cacheKey = PRESIGNED_URL_CACHE_PREFIX + objectKey;
    String cached = redis.opsForValue().get(cacheKey);
    if (cached != null) {
        return cached;
    }
    String presignedUrl = minioObjectStorage.presignedGetUrl(objectKey, PRESIGNED_URL_TTL);
    redis.opsForValue().set(cacheKey, presignedUrl, PRESIGNED_URL_CACHE_TTL);
    return presignedUrl;
}
```

### 전체적으로 배운 것

- 스토리지 URL을 그대로 클라이언트에 내려주기 전에, "이 URL만으로 접근 제어가 되는가"를 먼저 확인해야 합니다.
  버킷 정책과 URL 발급 방식은 항상 세트로 맞춰야 합니다.
- 트랜잭션 범위는 최소 단위로 쪼개야 합니다. 특히 네트워크 I/O는 트랜잭션 안에 들어가지 않도록 별도 컴포넌트로 분리하는 게 안전합니다.
- "인증이 필요하다"가 "서버를 거쳐야 한다"를 뜻하지는 않습니다. presigned URL처럼 발급 시점에만 권한을 확인하고
  실제 전송은 클라이언트가 스토리지와 직접 하도록 넘기는 방법도 있습니다.
- 캐시를 도입할 때는 캐시 TTL을 원본 자원의 TTL보다 짧게 잡아야, 캐시가 만료된 자원을 돌려주는 경우를 막을 수 있습니다.

### 관련 커밋

| 커밋 | 내용 |
| --- | --- |
| `fcecdab` | fix: 사진 조회시 인증 문제 해결 (1차 — 서버 경유 byte 응답 도입) |
| `d810df1` | fix: MinIO 다운로드를 데이터베이스 트랜잭션 밖으로 분리 (2차) |
| `7df843b` | fix: front에 byte로 사진 보내는 로직을 presigned url 방식으로 변경 (3차) |
| `6aefc67` | feat: 사진 presigned URL 캐싱 적용 (4차) |

---

## 김동건 — (작성 예정)

본인이 해결한 문제를 위 형식(문제 상황 → 원인 → 해결 → 배운 점 → 관련 커밋)에 맞춰 이 자리에 추가해주세요.

## 이동희 — (작성 예정)

본인이 해결한 문제를 위 형식(문제 상황 → 원인 → 해결 → 배운 점 → 관련 커밋)에 맞춰 이 자리에 추가해주세요.
