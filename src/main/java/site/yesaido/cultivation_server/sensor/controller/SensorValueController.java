package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.dto.response.influx.*;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.service.SensorRedisCacheService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/{cultivation-id}/sensor-values")
public class SensorValueController {

    private final InfluxService influxService;
    private final SensorRedisCacheService sensorRedisCacheService;
    private final CultivationMemberService cultivationMemberService;

    @Value("${sensor-cache.freshness-seconds:3}")
    private long freshnessSeconds;

    @GetMapping("/trend")
    public ResponseEntity<SensorTrendPointListResponse> getTrend(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestParam(name = "device-eui", required = true) String deviceEui,
            @RequestParam(name = "sensor-type", required = true) String sensorType,
            @RequestParam(name = "unit", required = true) String unit,
            @RequestHeader(name = "X-User-Id") Long userId
    ) {
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        SensorTrendPointListResponse trend;
        try {
            trend = sensorRedisCacheService.findTrend(cultivationId, deviceEui, sensorType, unit);
        } catch (RuntimeException e) {
            trend = null;
        }
        if (trend == null) {
            trend = influxService.findTrend(cultivationId, deviceEui, sensorType, unit);
        }
        return ResponseEntity.ok(trend);
    }

    @GetMapping
    public ResponseEntity<LatestSensorValueListResponse> getLatest(@PathVariable("cultivation-id") Long cultivationId,
                                                                   @RequestHeader("X-User-Id") Long userId,
                                                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        LatestSensorValueListResponse response;
        try {
            var cached = sensorRedisCacheService.findLatest(cultivationId,
                    java.time.Duration.ofSeconds(freshnessSeconds));
            LatestSensorValueListResponse source = influxService.findLatestByCultivationId(cultivationId);
            response = mergeLatestValues(source, cached);
        } catch (RuntimeException e) {
            response = influxService.findLatestByCultivationId(cultivationId);
        }
        return ResponseEntity.ok(response);
    }

    private LatestSensorValueListResponse mergeLatestValues(
            LatestSensorValueListResponse source,
            List<LatestSensorValueResponse> cached
    ) {
        Map<String, LatestSensorValueResponse> merged = new LinkedHashMap<>();
        if (source != null && source.latestSensorValueResponses() != null) {
            source.latestSensorValueResponses().forEach(value -> merged.put(sensorKey(value), value));
        }
        if (cached != null) {
            cached.forEach(value -> merged.put(sensorKey(value), value));
        }
        return new LatestSensorValueListResponse(List.copyOf(merged.values()));
    }

    private String sensorKey(LatestSensorValueResponse value) {
        return value.deviceEui() + "|" + value.sensorType() + "|" + value.unit();
    }


    @GetMapping("/average")
    public ResponseEntity<SensorTypeAverageListResponse> getAverage(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestHeader("X-User-Id") Long userId
    ){
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        List<SensorTypeAverageResponse> average = influxService.findAverageByCultivationId(cultivationId);
        return ResponseEntity.ok(new SensorTypeAverageListResponse(average));
    }
}
