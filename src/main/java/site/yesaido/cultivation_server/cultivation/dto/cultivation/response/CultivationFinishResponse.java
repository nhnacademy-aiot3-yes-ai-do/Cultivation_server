package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;

import java.time.LocalDateTime;

public record CultivationFinishResponse(
        Long cultivationId,
        CultivationStatus status,
        LocalDateTime finishedAt
) {}
