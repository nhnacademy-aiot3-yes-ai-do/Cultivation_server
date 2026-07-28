package site.yesaido.cultivation_server.dto.cultivationmember.response;

import site.yesaido.cultivation_server.entity.cultivationmember.MemberRole;

import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        Long userId,
        MemberRole role,
        LocalDateTime joinedAt
) {}
