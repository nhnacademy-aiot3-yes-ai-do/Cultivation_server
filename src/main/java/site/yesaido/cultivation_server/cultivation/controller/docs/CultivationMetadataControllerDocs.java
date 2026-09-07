package site.yesaido.cultivation_server.cultivation.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataResponse;

/**
 * {@code CultivationMetadataController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배 메타데이터", description = "재배 목록/단건의 요약 메타데이터 조회")
public interface CultivationMetadataControllerDocs {

    @Operation(summary = "재배 메타데이터 목록 조회", description = "요청자가 속한 재배들의 요약 메타데이터를 반환합니다.")
    ResponseEntity<CultivationMetadataListResponse> getList(Long userId);

    @Operation(summary = "재배 메타데이터 단건 조회", description = "특정 재배의 요약 메타데이터를 반환합니다.")
    ResponseEntity<CultivationMetadataResponse> get(
            Long userId,
            @Parameter(description = "재배 ID") Long cultivationId);
}
