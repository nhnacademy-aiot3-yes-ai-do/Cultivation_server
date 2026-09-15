# 04. 인증과 권한

## 인증은 Gateway가 담당합니다

Cultivation Server에는 로그인도, JWT 파싱도, Spring Security 설정도 없습니다.
인증은 전적으로 API Gateway의 책임이고, 이 서비스는 **Gateway가 넣어준 헤더를 신뢰**합니다.

```
Client ──JWT──▶ API Gateway ──검증──▶ X-User-Id 주입 ──▶ Cultivation Server
```

| 헤더 | 필수 | 타입 | 설명 |
| --- | --- | --- | --- |
| `X-User-Id` | ✅ | Long | 요청자의 사용자 ID |
| `X-User-Role` | ❌ | String | 시스템 역할. `ADMIN`이면 멤버십 검사를 우회 |

컨트롤러는 이 값을 `@RequestHeader`로 받습니다.

```java
public ResponseEntity<CultivationDetailResponse> getCultivation(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader(value = "X-User-Role", required = false) String role,
        @PathVariable("cultivation-id") Long cultivationId) { ... }
```

> **보안상 전제** 이 구조는 `X-User-Id` 헤더를 외부에서 직접 주입할 수 없다는 전제 위에 서 있습니다.
> Gateway를 우회해 서비스에 직접 접근할 수 있는 경로가 열려 있으면 인증이 무력화됩니다.
> 배포 환경에서 서비스 포트가 클러스터 외부로 노출되지 않도록 해야 합니다.

---

## 두 층위의 권한

권한은 두 가지가 따로 존재하며 혼동하기 쉽습니다.

| 층위 | 값 | 출처 | 범위 |
| --- | --- | --- | --- |
| **시스템 역할** | `ADMIN` 등 | `X-User-Role` 헤더 | 서비스 전역 |
| **재배지 역할** | `OWNER` / `MANAGER` / `MEMBER` | `cultivation_member` 테이블 | 특정 재배지 안에서만 |

`ADMIN`은 **멤버가 아니어도 조회할 수 있게** 해주는 통과권입니다.
멤버십 검사를 우회할 뿐, `OWNER`만 할 수 있는 동작(멤버 관리 등)까지 열어주지는 않습니다.

```java
public void existCultivationMember(Long cultivationId, Long userId, String role) {
    if (ADMIN_ROLE.equals(role)) {
        return;                    // ADMIN 은 멤버십 검사 통과
    }
    if (!cultivationMemberRepository.existsByCultivationIdAndUserId(cultivationId, userId)) {
        throw new CultivationAccessDeniedException(cultivationId);
    }
}
```

---

## 재배지 역할

| 역할 | 인원 | 할 수 있는 일 |
| --- | --- | --- |
| `OWNER` | 재배지당 1명 | 아래 모든 것 + 멤버 추가·역할 변경·삭제, 소유권 위임, 재배 종료·삭제 |
| `MANAGER` | 제한 없음 | 아래 모든 것 + 센서 등록·삭제, 환경 설정 수정, 수확 등록, 수확 모드 전환 |
| `MEMBER` | 제한 없음 | 조회 (재배 상세, 멤버 목록, 사진, 수확 기록, 센서값, 환경 준수율), 사진 업로드·삭제 |

역할은 포함 관계입니다. `OWNER`는 `MANAGER`가 할 수 있는 모든 일을 할 수 있습니다.

---

## 검증 메서드

권한 검증은 `CultivationMemberService`의 세 메서드와 공통 가드로 통일되어 있습니다.

| 메서드 | 통과 조건 | 실패 시 |
| --- | --- | --- |
| `existCultivationMember(id, userId, role)` | 멤버이거나 `ADMIN` | `CultivationAccessDeniedException` (403) |
| `verifyManagerAccess(id, userId)` | 역할이 `MEMBER`가 아님 (= `MANAGER` 또는 `OWNER`) | `CultivationAccessDeniedException` (403) |
| `verifyOwnerAccess(id, userId, role)` | 역할이 `OWNER` | `CultivationAccessDeniedException` (403) |

`CultivationAccessGuard.requireMember(id, userId, role)`는
"재배지 존재 확인 + 멤버십 확인"을 한 번에 처리하고 `Cultivation` 엔티티를 돌려줍니다.
재배지가 없으면 `CultivationNotFoundException`(404)입니다.

```java
Cultivation cultivation = accessGuard.requireMember(cultivationId, userId, role);
```

---

## 동작별 요구 권한

실제 코드에서 호출하는 검증 메서드 기준입니다.

### 재배

| 동작 | 검증 | 요구 권한 |
| --- | --- | --- |
| 재배 생성 | — | 인증만 |
| 내 재배 목록 | — (본인 멤버십 기준 조회) | 인증만 |
| 재배 상세 조회 | 조회 결과의 `myRole`이 `null`이 아닐 것 (또는 `ADMIN`) | 멤버 |
| 재배 이력 | — (본인 기준 조회) | 인증만 |
| **재배 종료** | `verifyOwnerAccess` | **OWNER** |
| **재배 삭제** | `verifyOwnerAccess(role)` | **OWNER** (`ADMIN` 우회 가능) |
| **수확 모드 전환** | `verifyManagerAccess` | **MANAGER↑** |

### 멤버

| 동작 | 검증 | 요구 권한 |
| --- | --- | --- |
| 멤버 목록 조회 | `existCultivationMember(role)` | 멤버 |
| 멤버 추가 | `requireOwner` | **OWNER** |
| 멤버 역할 변경 | `requireOwner` | **OWNER** |
| 멤버 삭제 | `requireOwner` | **OWNER** |
| 소유권 위임 | 요청자가 `OWNER`인지 직접 확인 | **OWNER** |

### 사진

| 동작 | 검증 | 요구 권한 |
| --- | --- | --- |
| 업로드 | `requireMember` | 멤버 |
| 목록 조회 | `requireMember(role)` | 멤버 (`ADMIN` 우회 가능) |
| 삭제 | `requireMember` | 멤버 |

### 수확

| 동작 | 검증 | 요구 권한 |
| --- | --- | --- |
| 수확 등록 | `verifyManagerAccess` | **MANAGER↑** |
| 수확 조회 | `requireMember` | 멤버 |
| 품질 점수 갱신 (공개 API) | `verifyManagerAccess` | **MANAGER↑** |
| 품질 점수 갱신 (내부 API) | 없음 | 서비스 간 호출 전용 |

### 센서 · 환경 설정

| 동작 | 검증 | 요구 권한 |
| --- | --- | --- |
| 센서 등록 | `verifyManagerAccess` | **MANAGER↑** |
| 센서 삭제 | `verifyManagerAccess` | **MANAGER↑** |
| 환경 설정 수정 | `verifyManagerAccess` | **MANAGER↑** |
| 센서 목록·센서값 조회 | `existCultivationMember(role)` | 멤버 |
| 환경 준수율 조회 | 멤버십 확인 | 멤버 |

---

## 멤버 관리 규칙

### 역할 변경

```java
if (request.role() == MemberRole.OWNER) {
    throw new InvalidMemberRoleException();
}
requireOwner(cultivationId, requesterId);
```

역할 변경 API로는 **`OWNER`를 지정할 수 없습니다.** 소유자를 바꾸려면 소유권 위임 API를 써야 합니다.
`OWNER`가 둘 이상 생기는 상태를 원천 차단하기 위한 제약입니다.

### 소유권 위임

`PUT /api/v1/cultivations/{cultivation-id}/owner`

1. 자기 자신에게 위임하면 `InvalidOwnershipTransferException`
2. 요청자를 **비관적 락**(`findByCultivationIdAndUserIdForUpdate`)으로 조회 — 동시 위임 방지
3. 요청자가 `OWNER`가 아니면 `CultivationAccessDeniedException`
4. 대상이 멤버가 아니면 `CultivationMemberNotFoundException`
5. 기존 소유자 → `MANAGER`, 새 소유자 → `OWNER`
6. `Cultivation.userId`도 새 소유자로 갱신

```
전:  A(OWNER)  B(MEMBER)        Cultivation.userId = A
후:  A(MANAGER) B(OWNER)        Cultivation.userId = B
```

기존 소유자가 재배지에서 빠지지 않고 `MANAGER`로 남는다는 점에 유의하세요.

---

## 오류 응답

권한 관련 예외는 `GlobalExceptionHandler`가 RFC 7807 형식으로 변환합니다.

| 예외 | HTTP | 발생 상황 |
| --- | --- | --- |
| `CultivationNotFoundException` | 404 | 재배지가 없거나 삭제됨 |
| `CultivationAccessDeniedException` | 403 | 멤버가 아니거나 역할이 부족함 |
| `CultivationMemberNotFoundException` | 404 | 대상 사용자가 해당 재배지 멤버가 아님 |
| `InvalidMemberRoleException` | 400 | 역할 변경으로 `OWNER`를 지정하려 함 |
| `InvalidOwnershipTransferException` | 400 | 자기 자신에게 소유권 위임 시도 |

응답 본문에는 `status` · `title` · `detail` · `code` · `instance`가 담깁니다.

> 존재하지 않는 재배지(404)와 권한 없는 재배지(403)를 구분해 응답하므로,
> 재배지 ID의 존재 여부가 외부에 드러납니다. 필요하다면 두 경우를 404로 통일하는 것을 검토하세요.
