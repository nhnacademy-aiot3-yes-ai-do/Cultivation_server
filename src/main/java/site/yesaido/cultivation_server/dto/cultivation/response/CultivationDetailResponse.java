package site.yesaido.cultivation_server.dto.cultivation.response;

import site.yesaido.cultivation_server.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;

import java.time.LocalDateTime;

public record CultivationDetailResponse(
        Long cultivationId,
        String name,
        Long mushroomId,
        CultivationStatus status,
        CultivationMode mode,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
