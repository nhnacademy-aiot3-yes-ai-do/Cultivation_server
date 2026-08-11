package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sensor-types")
public class SensorTypeController {
    private final SensorTypeService sensorTypeService;

    @GetMapping
    public ResponseEntity<SensorTypeInfoListResponse> getAll() {
        SensorTypeInfoListResponse all = sensorTypeService.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(all);
    }
}
