package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;

public record CultivationModeChangeResponse(
        Long cultivationId,
        CultivationMode mode
) {
}
