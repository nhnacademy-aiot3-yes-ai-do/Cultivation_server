package site.yesaido.cultivation_server.dto.cultivation.response;

import site.yesaido.cultivation_server.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;

import java.time.LocalDateTime;

public record CultivationSummaryResponse(
        Long cultivationId,
        String name,
        Long mushroomId,
        CultivationStatus status,
        CultivationMode mode,
        LocalDateTime createdAt
) {
}
