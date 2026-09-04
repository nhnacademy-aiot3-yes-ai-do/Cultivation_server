package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sensors")
public class UserSensorCatalogController {

    private final CultivationSensorFacade cultivationSensorFacade;

    @GetMapping("/reusable")
    public ResponseEntity<ReusableCultivationSensorListResponse> getReusableSensors(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("exclude-cultivation-id") long excludedCultivationId
    ) {
        return ResponseEntity.ok(
                cultivationSensorFacade.findReusableSensors(userId, excludedCultivationId)
        );
    }
}
