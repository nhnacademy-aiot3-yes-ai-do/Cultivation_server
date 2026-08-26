package site.yesaido.cultivation_server.sensor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

import java.net.URI;

@RequestMapping("/api/v1/cultivations/{cultivation-id}/sensors")
@RestController
@RequiredArgsConstructor
public class CultivationSensorController {

    private final CultivationSensorFacade cultivationSensorFacade;

    @PostMapping
    public ResponseEntity<Void> register(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("cultivation-id") long cultivationId,
            @Valid @RequestBody CreateCultivationSensorRequest request
    ) {
        long sensorId = cultivationSensorFacade.register(userId, cultivationId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{sensor-id}")
                .buildAndExpand(sensorId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{sensor-id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("cultivation-id") long cultivationId,
            @PathVariable("sensor-id") long sensorId
    ) {
        cultivationSensorFacade.delete(userId, cultivationId, sensorId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CultivationSensorListResponse> getAllCultivationSensor(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("cultivation-id") long cultivationId
    ) {
        return ResponseEntity.ok(
                cultivationSensorFacade.findAll(userId, cultivationId, role)
        );
    }

}
