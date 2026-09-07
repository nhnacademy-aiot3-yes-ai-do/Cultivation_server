package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;

/**
 * {@code CultivationSensorController}의 OpenAPI 문서 정의.
 */
@Tag(name = "재배 센서", description = "재배별 센서 장치 등록 · 목록 · 삭제")
public interface CultivationSensorControllerDocs {

    @Operation(summary = "센서 등록",
            description = "재배에 센서 장치와 측정 채널을 등록합니다. `Location` 헤더로 생성된 센서 URI를 반환합니다.")
    @ApiResponse(responseCode = "201", description = "등록됨")
    ResponseEntity<Void> register(
            Long userId,
            @Parameter(description = "재배 ID") long cultivationId,
            CreateCultivationSensorRequest request);

    @Operation(summary = "센서 삭제", description = "재배에서 센서 장치를 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    ResponseEntity<Void> delete(
            Long userId,
            @Parameter(description = "재배 ID") long cultivationId,
            @Parameter(description = "센서 ID") long sensorId);

    @Operation(summary = "센서 목록 조회", description = "재배에 등록된 센서 장치와 측정 채널 목록을 반환합니다.")
    ResponseEntity<CultivationSensorListResponse> getAllCultivationSensor(
            Long userId,
            String role,
            @Parameter(description = "재배 ID") long cultivationId);
}
