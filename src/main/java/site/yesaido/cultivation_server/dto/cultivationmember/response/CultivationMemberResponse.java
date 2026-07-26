package site.yesaido.cultivation_server.dto.cultivationmember.response;

import site.yesaido.cultivation_server.entity.cultivationmember.MemberRole;

import java.time.LocalDateTime;

public record CultivationMemberResponse(
        Long userId,
        MemberRole role,
        LocalDateTime joinedAt
) {
}
