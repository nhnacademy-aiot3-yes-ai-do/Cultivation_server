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

## 이동희 — 경작 센서·환경설정·서비스 연동

경작 센서 등록과 센서 타입 연결, 경작지별 환경 기준 관리, Rule Engine·Data Generator에 변경 사항을 전달하는 부분을 맡았습니다.
가장 많이 고민한 부분은 **어떤 데이터를 함께 저장해야 하는지**, 그리고 **다른 서비스에 언제, 어떤 기준으로 알려야 하는지**였습니다.

### 1. 경작지의 임계값을 기기마다 설정하는 것으로 이해한 문제

**문제 상황**
초기 기기 등록 화면에서는 센서를 추가할 때마다 임계값을 입력할 수 있었습니다.
그 화면을 기준으로 백엔드끼리 논의하다 보니, 같은 경작지에 기기가 여러 개 있을 때 Rule Engine이 어떤 임계값을 사용해야 하는지 혼동이 생겼습니다.

**원인**
DB에서 환경 기준은 `environment_setting`의 `(cultivation_id, sensor_type_id)` 조합으로 관리합니다.
즉 기기별 설정이 아니라 **경작지 안에서 같은 센서 타입이 공유하는 설정**인데, 화면의 입력 단위와 데이터의 소유 단위가 달랐습니다.
기기 등록 요청으로 기존 환경 기준을 갱신할 수 있는 구조까지 겹치면서, 단순히 센서를 추가하는 것과 경작지 기준을 바꾸는 것이 섞였습니다.

**해결**
팀에서 경작지 생성 시 환경 기준을 정하고, 이후 기기 등록에서는 기존 기준을 가져와 사용할 센서 종류를 선택하는 흐름으로 정리했습니다.
프론트에서는 기존 기준을 기기마다 다시 입력하지 않도록 수정했고, Cultivation에서는 `CultivationCreationFacade`로 경작지와 초기 환경설정을 함께 저장하도록 연결했습니다.
환경 기준을 의도적으로 바꿀 때 사용할 수정 API도 별도로 추가했습니다.
다만 현재 기기 등록 API에도 환경설정을 반영하는 `apply()` 호출은 남아 있으므로, 기존 기준 재사용은 화면 흐름에서 정리한 부분이며 서버가 변경 자체를 금지하는 구조는 아닙니다.

**배운 점**
화면에 입력란이 있다고 해서 그 값이 해당 기기의 소유라고 생각하면 안 됐습니다. 먼저 데이터의 기준과 변경 주체를 맞췄어야 했습니다.
또 서비스 간에 문제 상황만 공유하고 수정 결과를 제때 공유하지 않아, 서로 다른 상태를 전제로 다시 논의하는 데 시간이 들었습니다.
앞으로는 변경한 필드, 적용한 경로, 상대 서비스에서 확인할 동작까지 함께 전달하려고 합니다.

### 2. 센서 등록의 트랜잭션 범위와 외부 이벤트 전송 시점

**문제 상황**
센서 하나를 등록해도 장치 정보 저장, 센서 타입 연결, 환경설정 반영이 함께 필요했습니다.
초기 설계에서는 한 Service에 여러 Repository와 검증 책임이 몰렸고, 어느 단계가 실패했을 때 어디까지 되돌려야 하는지도 정리할 필요가 있었습니다.
여기에 Rule Engine·Data Generator로 정보를 보내는 과정이 붙으면서, DB 저장과 메시지 전송의 경계도 문제가 됐습니다.

**원인**
등록 절차를 조합하는 책임과 각 도메인의 규칙을 처리하는 책임을 같은 곳에 넣으려 했습니다.
또 DB 커밋 전에 메시지를 보내면, 이후 DB가 롤백돼도 다른 서비스에는 등록됐다는 정보가 남을 수 있었습니다.

**해결**
`CultivationSensorFacade`가 등록 순서와 하나의 DB 트랜잭션을 맡고, 장치 등록·타입 연결·환경설정 검증은 각각의 Service에 나눴습니다.
타입 정보는 조회 결과의 순서에 의존하지 않고 ID를 키로 하는 Map으로 연결했습니다.
Facade에서는 애플리케이션 이벤트를 발행하고, RabbitMQ 전송은 `@TransactionalEventListener(phase = AFTER_COMMIT)`과 `@Async`를 붙인 리스너에서 처리했습니다.

```text
Facade 트랜잭션: 장치 저장 → 타입 연결 → 환경설정 반영 → 애플리케이션 이벤트 발행
DB 커밋 성공: 리스너가 RabbitMQ로 전송
DB 롤백: 커밋 후 리스너는 실행하지 않음
```

이 구조는 DB 저장 실패 후 메시지만 나가는 문제를 막기 위한 것입니다.
DB 커밋 이후 전송 실패까지 원자적으로 해결한 것은 아니며, 비동기로 보내는 임계값 이벤트와 센서 정보 이벤트의 도착 순서도 보장하지 않습니다.

**배운 점**
Facade를 단순히 Service를 한 번 더 감싸는 계층으로 보기보다, 하나의 사용자 작업이 끝나는 범위와 실행 순서를 정하는 곳으로 이해하게 됐습니다.
또 로컬 DB의 롤백과 다른 서비스까지 포함한 정합성은 별도로 생각해야 한다는 점을 배웠습니다.

### 3. 온도 임계값을 수정해도 화씨 기준은 이전 값으로 남는 문제

**문제 상황**
온도는 섭씨와 화씨를 함께 지원하는데, 한쪽 임계값을 바꾸면 다른 쪽은 이전 값으로 남을 수 있었습니다.
생성·등록 경로에 변환을 넣은 뒤에도 수확 모드 전환과 개별 임계값 수정 경로에서 같은 처리가 빠져 있었습니다.

**원인**
온도 변환을 각 기능에서 따로 처리하면, 같은 환경설정을 변경하는 경로 중 일부가 누락됐습니다.
특히 개별 수정은 요청에 들어온 센서 타입 한 개만 갱신하고, 해당 단위의 범위만 이벤트에 담고 있었습니다.

**해결**
`EnvironmentSettingPreparationService`에서 온도 요청 하나를 섭씨·화씨 두 설정으로 준비하도록 공통화했습니다.
경작지 생성, 기기 등록, 수확 모드 전환, 개별 임계값 수정이 모두 이 처리를 거치도록 연결했습니다.
예를 들어 섭씨 `18~24°C`로 수정하면 화씨 `64.4~75.2°F`도 함께 저장하고, 변경 이벤트에도 두 단위를 담습니다.
그래서 기본 환경 항목은 온도·습도·CO₂·조도 네 종류지만, 온도를 두 단위로 저장하면 환경설정은 다섯 건이 됩니다.
서로 다른 기준을 동시에 입력하지 않도록 한 요청에서 섭씨·화씨를 함께 지정하는 경우는 거부하고, 단위 변환과 수정 이벤트를 확인하는 테스트도 보강했습니다.

**배운 점**
공통 함수를 만드는 것만으로 일관성이 생기지는 않았습니다. 같은 값을 변경하는 모든 진입점을 찾아 실제로 연결해야 했습니다.
생성에 성공하는지만 보지 않고 수정과 모드 전환 이후에도 두 단위가 같은 범위를 나타내는지 확인해야 했습니다.

### 4. 삭제 메서드는 실행되지만 DB에는 삭제 상태가 반영되지 않는 문제

**문제 상황**
경작지 삭제에서 엔티티의 삭제 메서드를 호출했는데도, 변경 감지로 저장돼야 할 상태가 DB에 반영되지 않는 문제가 있었습니다.

**원인**
`CultivationServiceImpl`에는 클래스 단위로 `@Transactional(readOnly = true)`가 붙어 있었습니다.
당시 인자 두 개짜리 삭제 메서드에는 쓰기 트랜잭션이 있었지만, 컨트롤러에서 호출하는 역할 정보 포함 메서드에는 없었습니다.
실제로 사용한 진입점이 읽기 전용 트랜잭션으로 실행되면서, 엔티티 값을 바꿔도 의도한 UPDATE로 이어지지 않았습니다.

**해결**
실제 삭제를 수행하는 공개 메서드에 `@Transactional`을 명시했습니다.
역할 정보 없는 삭제 경로도 `deleteWithoutRole()`로 구분하고, 조회·권한 확인·삭제 상태 변경이 쓰기 트랜잭션 안에서 처리되도록 정리했습니다.

**배운 점**
같은 클래스의 비슷한 메서드에 트랜잭션이 붙어 있다는 것만 확인해서는 부족했습니다.
컨트롤러가 어떤 메서드로 들어오는지, 그 진입점에 어떤 트랜잭션 설정이 적용되는지까지 따라가야 했습니다.

### 5. 재배 종료 후 센서 해제와 재사용 조건이 맞지 않는 문제

**문제 상황**
재배지를 종료하거나 삭제하는 경로와 센서를 해제하는 경로가 완전히 연결돼 있지 않았습니다.
재사용 센서 조회도 다른 경작지에서 아직 사용 중인 기기를 걸러내지 않아, 같은 EUI를 다시 등록할 수 있는지에 대한 기준이 맞지 않았습니다.

**원인**
경작지 상태 변경만으로 센서 등록 관계까지 정리됐다고 볼 수 없는데, 두 수명주기를 따로 처리했습니다.
같은 경작지 안의 EUI 중복 검사만으로는 다른 경작지에서 사용 중인 기기까지 막을 수 없었습니다.

**해결**
종료·삭제 경로에서도 `CultivationSensorFacade.deleteAll()`을 호출해 센서를 소프트 삭제하고, 커밋 후 센서 삭제 이벤트와 빈 임계값 목록을 전달하도록 연결했습니다.
DB의 환경설정 기록은 남기면서, 연동 서비스에는 해당 경작지의 제어 기준과 센서 등록을 해제하도록 알리는 방식입니다.
새 등록이나 복구 전에 다른 경작지에 삭제되지 않은 동일 EUI가 있는지 검사하고, 재사용 목록에도 같은 조건을 적용했습니다.
종료·삭제·수확 처리, 센서가 없는 경우, 정리 도중 실패하면 DB 변경이 롤백되고 커밋 후 리스너가 실행되지 않는 경우를 통합 테스트에 추가했습니다.

**배운 점**
기기를 다시 등록할 수 있게 하려면 등록 API만 고칠 것이 아니라, 기존 사용을 끝내는 경로부터 정리해야 했습니다.
화면에 보여주는 재사용 가능 목록과 실제 등록 시 검증도 같은 규칙을 사용해야 합니다.

### 관련 커밋

| 커밋 | 내용 |
| --- | --- |
| `c4132b4` | 센서 등록·삭제와 Facade, 타입 연결·환경설정 Service 구현 |
| `1544d1b` | 센서 등록·삭제 이벤트 및 커밋 후 비동기 RabbitMQ 전송 도입 |
| `556c9f8` | 임계값 변경 이벤트와 센서 정보 이벤트 연동 보강 |
| `a593059` | 경작지 생성과 초기 환경설정을 묶는 CultivationCreationFacade 추가 |
| `1679403` | 환경설정 수정 API와 임계값 변경 이벤트 추가 |
| `7e1c9ca` | EnvironmentSettingPreparationService와 섭씨·화씨 공통 처리 도입 |
| `e38353a`, `de524d0` | 수확 모드 전환에 공통 처리 연결 및 테스트 수정 |
| `d0d2fc4`, `7ddb96b` | 개별 임계값 수정 시 섭씨·화씨 동시 갱신 및 테스트 보강 |
| `21d6aca`, `626d7f4`, `a0049ed` | 경작지 소프트 삭제의 쓰기 트랜잭션 적용과 호출 경로 정리 |
| `90bb56a` | 수확 종료 시 센서 일괄 해제와 삭제 이벤트 연결 |
| `b95370f` | 다른 경작지의 EUI 사용 여부 검사, 종료·삭제 시 센서 정리 및 통합 테스트 추가 |
