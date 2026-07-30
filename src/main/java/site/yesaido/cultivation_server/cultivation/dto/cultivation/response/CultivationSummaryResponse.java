package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;

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
