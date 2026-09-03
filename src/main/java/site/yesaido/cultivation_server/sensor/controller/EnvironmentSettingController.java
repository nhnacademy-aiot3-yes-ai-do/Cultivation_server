package site.yesaido.cultivation_server.sensor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/{cultivation-id}/environment-settings")
public class EnvironmentSettingController {

    private final CultivationSensorFacade cultivationSensorFacade;

    @PutMapping
    public ResponseEntity<Void> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("cultivation-id") long cultivationId,
            @Valid @RequestBody EnvironmentSettingRequest request
    ) {
        cultivationSensorFacade.updateEnvironmentSetting(userId, cultivationId, request);
        return ResponseEntity.noContent().build();
    }
}
