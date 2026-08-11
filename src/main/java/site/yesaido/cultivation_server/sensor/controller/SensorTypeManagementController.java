package site.yesaido.cultivation_server.sensor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/sensor-types")
public class SensorTypeManagementController {
    private final SensorTypeService sensorTypeService;

    @PostMapping
    public ResponseEntity<Void> registerSensorType(@Valid @RequestBody SensorTypeRequest request) {
        long savedId = sensorTypeService.registerSensorType(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{sensor-type-id}")
    public ResponseEntity<Void> updateSensorType(@PathVariable("sensor-type-id")Long id, @Valid @RequestBody SensorTypeRequest request) {
        sensorTypeService.updateSensorType(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{sensor-type-id}")
    public ResponseEntity<Void> deleteSensorType(@PathVariable("sensor-type-id")Long id) {
        sensorTypeService.deleteSensorType(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{sensor-type-id}")
    public ResponseEntity<SensorTypeInfoResponse> getSensorType(@PathVariable("sensor-type-id")Long id) {
        SensorTypeInfoResponse sensorType = sensorTypeService.getSensorTypeById(id);
        return ResponseEntity.status(HttpStatus.OK).body(sensorType);
    }
}
