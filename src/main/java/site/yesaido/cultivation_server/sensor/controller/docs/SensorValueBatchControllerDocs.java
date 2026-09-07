package site.yesaido.cultivation_server.sensor.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueBatchResponse;

/**
 * {@code SensorValueBatchController}의 OpenAPI 문서 정의.
 */
@Tag(name = "센서 측정값", description = "재배 센서 측정값 추이 · 최신값 · 24시간 평균 조회 (InfluxDB + Redis 캐시)")
public interface SensorValueBatchControllerDocs {

    @Operation(summary = "내 재배 센서 최신값 일괄 조회",
            description = "요청자가 속한 모든 재배의 센서 채널별 최신 측정값을 재배 ID별로 묶어 반환합니다. 캐시 조회 실패 시 503.")
    ResponseEntity<LatestSensorValueBatchResponse> getLatestForUser(Long userId);
}
