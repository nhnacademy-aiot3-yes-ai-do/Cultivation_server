package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;

/**
 * {@code SensorTypeManagementController}의 OpenAPI 문서 정의.
 */
@Tag(name = "관리자 - 센서 타입", description = "센서 타입 등록 · 수정 · 삭제 · 조회 (관리자 전용)")
public interface SensorTypeManagementControllerDocs {

    @Operation(summary = "센서 타입 등록", description = "새 센서 타입을 등록합니다. `Location` 헤더로 생성 URI를 반환합니다.")
    @ApiResponse(responseCode = "201", description = "등록됨")
    ResponseEntity<Void> registerSensorType(SensorTypeRequest request);

    @Operation(summary = "센서 타입 수정", description = "센서 타입 정보를 수정합니다.")
    @ApiResponse(responseCode = "204", description = "수정됨")
    ResponseEntity<Void> updateSensorType(
            @Parameter(description = "센서 타입 ID") Long id,
            SensorTypeRequest request);

    @Operation(summary = "센서 타입 삭제", description = "센서 타입을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    ResponseEntity<Void> deleteSensorType(@Parameter(description = "센서 타입 ID") Long id);

    @Operation(summary = "센서 타입 단건 조회", description = "ID로 센서 타입 정보를 조회합니다.")
    ResponseEntity<SensorTypeInfoResponse> getSensorType(@Parameter(description = "센서 타입 ID") Long id);
}
