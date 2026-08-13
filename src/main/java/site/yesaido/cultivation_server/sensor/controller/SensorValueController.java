package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.service.InfluxService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cultivations/{cultivation-id}/sensor-values")
public class SensorValueController {

    private final InfluxService influxService;
    private final CultivationMemberService cultivationMemberService;

    @GetMapping("/trend")
    public ResponseEntity<SensorTrendPointListResponse> getTrend(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestParam(name = "device-eui", required = true) String deviceEui,
            @RequestParam(name = "sensor-type", required = true) String sensorType,
            @RequestHeader(name = "X-User-Id") Long userId
    ) {
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        SensorTrendPointListResponse trend = influxService.findTrend(cultivationId, deviceEui, sensorType);
        return ResponseEntity.ok(trend);
    }
}
