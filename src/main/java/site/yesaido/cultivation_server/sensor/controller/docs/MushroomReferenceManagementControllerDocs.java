package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;

/**
 * {@code MushroomReferenceManagementController}의 OpenAPI 문서 정의.
 */
@Tag(name = "관리자 - 버섯 레퍼런스", description = "버섯 레퍼런스 등록 · 수정 · 삭제 · 조회 (관리자 전용)")
public interface MushroomReferenceManagementControllerDocs {

    @Operation(summary = "버섯 레퍼런스 등록", description = "새 버섯 레퍼런스를 등록합니다. `Location` 헤더로 생성 URI를 반환합니다.")
    @ApiResponse(responseCode = "201", description = "등록됨")
    ResponseEntity<Void> registerMushroomReference(MushroomReferenceRequest request);

    @Operation(summary = "버섯 레퍼런스 수정", description = "버섯 레퍼런스 정보를 수정합니다.")
    @ApiResponse(responseCode = "204", description = "수정됨")
    ResponseEntity<Void> updateMushroomReference(
            @Parameter(description = "버섯 레퍼런스 ID") Long id,
            MushroomReferenceRequest request);

    @Operation(summary = "버섯 레퍼런스 삭제", description = "버섯 레퍼런스를 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    ResponseEntity<Void> deleteMushroomReference(@Parameter(description = "버섯 레퍼런스 ID") Long id);

    @Operation(summary = "버섯 레퍼런스 단건 조회", description = "ID로 버섯 레퍼런스 정보를 조회합니다.")
    ResponseEntity<MushroomReferenceInfoResponse> getMushroomReference(@Parameter(description = "버섯 레퍼런스 ID") Long id);

    @Operation(summary = "버섯 레퍼런스 전체 조회", description = "등록된 모든 버섯 레퍼런스 정보를 반환합니다.")
    ResponseEntity<MushroomReferenceInfoListResponse> getAllMushroomReference();
}
