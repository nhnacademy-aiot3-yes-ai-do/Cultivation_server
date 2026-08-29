package site.yesaido.cultivation_server.cultivation.service;

import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;

public interface HarvestService {
    // 수확 기록
    HarvestCreateResponse createHarvest(Long cultivationId, Long userId, HarvestCreateRequest harvestCreateRequest);
    // 수확 상세 조회
    HarvestDetailResponse getHarvest(Long cultivationId, Long userId);
    // 상품 점수 / 등급 업데이트
    ProductScoreUpdateResponse updateProductScore(Long cultivationId, Long userId, ProductScoreUpdateRequest request);

    // ai-service 등 내부 서비스가 X-User-Id 없이 호출하는 메서드 (매니저 권한 검증 생략)
    ProductScoreUpdateResponse updateProductScoreInternal(Long cultivationId, ProductScoreUpdateRequest request);
}
