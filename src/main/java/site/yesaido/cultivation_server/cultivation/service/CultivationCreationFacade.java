package site.yesaido.cultivation_server.cultivation.service;

import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationCreateResponse;

public interface CultivationCreationFacade {
    CultivationCreateResponse create(Long userId, CultivationCreateRequest request);
}
