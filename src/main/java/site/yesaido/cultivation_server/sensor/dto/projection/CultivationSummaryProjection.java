package site.yesaido.cultivation_server.sensor.dto.projection;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.cultivationmember.MemberRole;

import java.time.LocalDateTime;

public record CultivationSummaryProjection(
        Long cultivationId,
        String name,
        Long mushroomId,
        CultivationStatus status,
        CultivationMode mode,
        Long memberCount,
        Long ownerUserId,
        MemberRole myRole,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}