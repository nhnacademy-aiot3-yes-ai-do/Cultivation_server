package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationModeChangeResponse;

public interface CultivationModeFacade {
    CultivationModeChangeResponse switchToHarvestMode(Long cultivationId, Long userId);
}
