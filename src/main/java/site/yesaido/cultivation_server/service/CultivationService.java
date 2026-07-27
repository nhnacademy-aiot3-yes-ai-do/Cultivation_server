package site.yesaido.cultivation_server.service;


import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationDetailResponse;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationSummaryResponse;

import java.util.List;

public interface CultivationService {
    CultivationCreateResponse create(CultivationCreateRequest request, Long userId);

    List<CultivationSummaryResponse> getCultivations(Long userId);

    CultivationDetailResponse getCultivation(Long userId, Long cultivationId);
}
