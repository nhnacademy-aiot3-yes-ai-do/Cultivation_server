package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.controller.docs.SensorValueControllerDocs;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorTypeResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorService;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.service.SensorLatestValueService;
import site.yesaido.cultivation_server.sensor.service.SensorTrendService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/{cultivation-id}/sensor-values")
public class SensorValueController implements SensorValueControllerDocs {
    private final InfluxService influxService;
    private final CultivationMemberService cultivationMemberService;
    private final CultivationSensorService cultivationSensorService;
    private final SensorLatestValueService sensorLatestValueService;
    private final SensorTrendService sensorTrendService;

    @Override
    @GetMapping("/trend")
    public ResponseEntity<SensorTrendPointListResponse> getTrend(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestParam(name = "device-eui") String deviceEui,
            @RequestParam(name = "sensor-type") String sensorType,
            @RequestParam(name = "unit") String unit,
            @RequestHeader(name = "X-User-Id") Long userId
    ) {
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        return ResponseEntity.ok(sensorTrendService.findTrend(cultivationId, deviceEui, sensorType, unit));
    }

    @Override
    @GetMapping
    public ResponseEntity<LatestSensorValueListResponse> getLatest(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        return sensorLatestValueService.getLatest(cultivationId);
    }

    @Override
    @GetMapping("/average")
    public ResponseEntity<SensorTypeAverageListResponse> getAverage(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        List<SensorTypeAverageResponse> averages = influxService.findAverageByCultivationIdForLast24Hours(cultivationId);
        Set<String> activeSensorTypes = cultivationSensorService.findAll(cultivationId).stream()
                .flatMap(sensor -> sensor.sensorTypes().stream())
                .map(CultivationSensorTypeResponse::type)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        List<SensorTypeAverageResponse> filteredAverages = averages.stream()
                .filter(avg -> avg.sensorType() != null && activeSensorTypes.contains(avg.sensorType().toUpperCase()))
                .toList();
        return ResponseEntity.ok(new SensorTypeAverageListResponse(filteredAverages));
    }
}
