package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;

/**
 * {@code MushroomReferenceController}의 OpenAPI 문서 정의.
 */
@Tag(name = "버섯 레퍼런스", description = "버섯 종류별 기준 정보(권장 환경 임계값 등) 조회")
public interface MushroomReferenceControllerDocs {

    @Operation(summary = "버섯 레퍼런스 전체 조회", description = "등록된 모든 버섯 레퍼런스 정보를 반환합니다.")
    ResponseEntity<MushroomReferenceInfoListResponse> getAllMushroomReference();
}
