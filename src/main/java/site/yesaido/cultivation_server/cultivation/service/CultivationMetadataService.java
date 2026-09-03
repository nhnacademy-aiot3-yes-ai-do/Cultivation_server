package site.yesaido.cultivation_server.cultivation.service;

import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataListResponse;

public interface CultivationMetadataService {
    CultivationMetadataResponse get(Long userId, Long cultivationId);
    CultivationMetadataListResponse getList(Long userId);
}
