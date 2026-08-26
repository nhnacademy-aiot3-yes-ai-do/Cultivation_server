package site.yesaido.cultivation_server.cultivation.service;

import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response.MemberListResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;

public interface CultivationMemberService {
    void addOwner(Cultivation cultivation, Long userId);
    void addMember(Long cultivationId, Long requesterId, MemberAddRequest request);
    MemberListResponse getMembers(Long cultivationId, Long requesterId);
    MemberListResponse getMembers(Long cultivationId, Long requesterId, String role);
    void updateMember(Long cultivationId, Long requesterId, Long targetUserId, MemberRoleUpdateRequest request);
    void removeMember(Long cultivationId, Long requesterId, Long targetUserId);

    // 소유권 이전
    void transferOwnership(Long cultivationId, Long requesterId, Long newUserId);

    // 멤버십 검증
    void existCultivationMember(Long cultivationId, Long userId);

    // 관리자는 비회원이어도 조회를 허용하기 위한 메서드
    void existCultivationMember(Long cultivationId, Long userId, String role);

    // 매니저 이상 권한 검증 (MEMBER 차단)
    void verifyManagerAccess(Long cultivationId, Long userId);

    // Owner 권한 검증 (OWNER만 허용)
    void verifyOwnerAccess(Long cultivationId, Long userId);
    // 시스템 관리자는 소유자가 아니여도 통과시키기 위한 메서드
    void verifyOwnerAccess(Long cultivationId, Long userId, String role);
}
