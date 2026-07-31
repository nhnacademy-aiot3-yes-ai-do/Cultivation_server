package site.yesaido.cultivation_server.cultivation.dto.cultivationmember.response;

import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;

import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        Long userId,
        String nickname,
        MemberRole role,
        LocalDateTime joinedAt
) {}
