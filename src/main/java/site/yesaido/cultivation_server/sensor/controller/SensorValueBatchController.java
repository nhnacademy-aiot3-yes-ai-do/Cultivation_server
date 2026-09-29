package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.config.SensorCacheProperties;
import site.yesaido.cultivation_server.sensor.controller.docs.SensorValueBatchControllerDocs;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueBatchResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.service.SensorLatestBatchService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/sensor-values")
public class SensorValueBatchController implements SensorValueBatchControllerDocs {
    private final SensorLatestBatchService sensorLatestBatchService;
    private final SensorCacheProperties sensorCacheProperties;

    @Override
    @GetMapping("/latest")
    public ResponseEntity<LatestSensorValueBatchResponse> getLatestForUser(
            @RequestHeader("X-User-Id") Long userId
    ) {
        Map<Long, List<LatestSensorValueResponse>> latest = sensorLatestBatchService.findLatestForUser(
                userId,
                Duration.ofSeconds(sensorCacheProperties.getFreshnessSeconds())
        );
        return ResponseEntity.ok(new LatestSensorValueBatchResponse(latest));
    }
}
