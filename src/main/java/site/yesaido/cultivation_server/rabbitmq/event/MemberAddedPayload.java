package site.yesaido.cultivation_server.rabbitmq.event;

import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;

public record MemberAddedPayload(
        Long cultivationId,
        String cultivationName,
        MemberRole role
) {
}
