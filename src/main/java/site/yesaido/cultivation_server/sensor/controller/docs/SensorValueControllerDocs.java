package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageListResponse;

/**
 * {@code SensorValueController}의 OpenAPI 문서 정의.
 */
@Tag(name = "센서 측정값", description = "재배 센서 측정값 추이 · 최신값 · 24시간 평균 조회 (InfluxDB + Redis 캐시)")
public interface SensorValueControllerDocs {

    @Operation(summary = "센서 추이 조회",
            description = "특정 센서 채널(device-eui + sensor-type + unit)의 최근 12시간 추이를 15분 평균으로 반환합니다. "
                    + "Redis 캐시 우선, 실패 시 InfluxDB 조회.")
    ResponseEntity<SensorTrendPointListResponse> getTrend(
            @Parameter(description = "재배 ID") Long cultivationId,
            @Parameter(description = "센서 장치 EUI") String deviceEui,
            @Parameter(description = "센서 타입 (예: TEMPERATURE)") String sensorType,
            @Parameter(description = "측정 단위 (예: °C)") String unit,
            Long userId);

    @Operation(summary = "센서 최신값 조회",
            description = "재배의 센서 채널별 최신 측정값을 반환합니다. 응답의 `status`로 신선도"
                    + "(FRESH/PARTIAL/SOURCE_FALLBACK/NO_DATA 등)를 알 수 있습니다. 캐시 조회 실패 시 503.")
    ResponseEntity<LatestSensorValueListResponse> getLatest(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId,
            String role);

    @Operation(summary = "센서 24시간 평균 조회", description = "재배 센서 타입별 최근 24시간 평균값을 InfluxDB에서 조회해 반환합니다.")
    ResponseEntity<SensorTypeAverageListResponse> getAverage(
            @Parameter(description = "재배 ID") Long cultivationId,
            Long userId);
}
