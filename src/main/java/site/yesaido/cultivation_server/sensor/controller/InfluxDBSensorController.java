package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.service.InfluxService;

import java.util.List;

@RequestMapping("/api/cultivations/{cultivation-id}/sensor-influx")
@RestController
@RequiredArgsConstructor
public class InfluxDBSensorController {
    private final InfluxService influxService;

    @PostMapping("/average")
    public ResponseEntity<List<SensorTypeAverageResponse>> sendAverageSensor(
            @PathVariable("cultivation-id") Long cultivationId)
    {
        return ResponseEntity.ok(influxService.findAverageByCultivationIdForLast24Hours(cultivationId));
    }
}
