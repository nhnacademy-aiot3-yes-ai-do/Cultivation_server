package site.yesaido.cultivation_server.service;


import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberAddRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.request.MemberRoleUpdateRequest;
import site.yesaido.cultivation_server.dto.cultivationmember.response.MemberResponse;
import site.yesaido.cultivation_server.entity.cultivation.Cultivation;

import java.util.List;

public interface CultivationMemberService {
    void addOwner(Cultivation cultivation, Long userId);
    void addMember(Long cultivationId, Long requesterId, MemberAddRequest request);
    List<MemberResponse> getMembers(Long cultivationId, Long requesterId);
    void updateMember(Long cultivationId, Long requesterId, Long targetUserId, MemberRoleUpdateRequest request);
    void removeMember(Long cultivationId, Long requesterId, Long targetUserId);
}
