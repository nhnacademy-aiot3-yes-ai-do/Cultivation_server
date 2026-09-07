package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;

/**
 * {@code HarvestController}의 OpenAPI 문서 정의.
 */
@Tag(name = "수확", description = "수확 등록 · 조회 및 상품성 점수 갱신")
public interface HarvestControllerDocs {

    @Operation(summary = "수확 등록", description = "수확 모드인 재배에 수확 정보를 등록합니다.")
    @ApiResponse(responseCode = "201", description = "등록됨")
    ResponseEntity<HarvestCreateResponse> createHarvest(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            HarvestCreateRequest request);

    @Operation(summary = "수확 조회", description = "재배의 수확 상세 정보를 반환합니다.")
    ResponseEntity<HarvestDetailResponse> getHarvest(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId);

    @Operation(summary = "상품성 점수 갱신", description = "수확물의 상품성 점수/등급을 갱신합니다.")
    ResponseEntity<ProductScoreUpdateResponse> updateProductScore(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            ProductScoreUpdateRequest request);
}
