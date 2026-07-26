package site.yesaido.cultivation_server.dto.cultivation.response;

import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;

import java.time.LocalDateTime;

public record CultivationFinishResponse(
        Long cultivationId,
        CultivationStatus status,
        LocalDateTime finishedAt
) {}
