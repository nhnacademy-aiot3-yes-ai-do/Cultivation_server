package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;

/**
 * {@code SensorTypeController}의 OpenAPI 문서 정의.
 */
@Tag(name = "센서 타입", description = "지원 센서 타입 목록 조회")
public interface SensorTypeControllerDocs {

    @Operation(summary = "센서 타입 목록 조회", description = "등록된 모든 센서 타입과 측정 단위를 반환합니다.")
    ResponseEntity<SensorTypeInfoListResponse> getAll();
}
