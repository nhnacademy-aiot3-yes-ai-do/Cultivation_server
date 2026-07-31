package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sensor-types")
public class SensorTypeController {
    private final SensorTypeService sensorTypeService;

    @PostMapping
    public ResponseEntity<Void> registerSensorType(@RequestBody SensorTypeRequest request) {
        long savedId = sensorTypeService.registerSensorType(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{sensor-type-id}")
    public ResponseEntity<Void> updateSensorType(@PathVariable("sensor-type-id")Long id,@RequestBody SensorTypeRequest request) {
        sensorTypeService.updateSensorType(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{sensor-type-id}")
    public ResponseEntity<Void> deleteSensorType(@PathVariable("sensor-type-id")Long id) {
        sensorTypeService.deleteSensorType(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<SensorTypeInfoListResponse> getAll() {
        SensorTypeInfoListResponse all = sensorTypeService.findAll();
        return ResponseEntity.status(200).body(all);
    }
}
