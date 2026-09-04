package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueBatchResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.service.SensorLatestBatchService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/sensor-values")
public class SensorValueBatchController {
    private final SensorLatestBatchService sensorLatestBatchService;

    @Value("${sensor-cache.freshness-seconds:3}")
    private long freshnessSeconds;

    @GetMapping("/latest")
    public ResponseEntity<LatestSensorValueBatchResponse> getLatestForUser(
            @RequestHeader("X-User-Id") Long userId
    ) {
        try {
            Map<Long, List<LatestSensorValueResponse>> latest = sensorLatestBatchService.findLatestForUser(
                    userId,
                    Duration.ofSeconds(freshnessSeconds)
            );
            return ResponseEntity.ok(new LatestSensorValueBatchResponse(latest));
        } catch (DataAccessException exception) {
            log.warn("사용자 센서 최신값 batch 조회 실패: stage=redis-latest-batch, exception={}",
                    exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new LatestSensorValueBatchResponse(Map.of()));
        }
    }
}
