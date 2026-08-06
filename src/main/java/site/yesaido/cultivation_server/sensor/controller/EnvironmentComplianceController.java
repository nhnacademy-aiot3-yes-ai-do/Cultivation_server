package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

@RestController
@RequestMapping("/api/cultivations/{cultivation-id}/environment-compliance")
@RequiredArgsConstructor
public class EnvironmentComplianceController {
    private final EnvironmentComplianceService environmentComplianceService;

    @GetMapping
    public ResponseEntity<EnvironmentComplianceResponse> get(@PathVariable("cultivation-id") Long cultivationId) {
        return ResponseEntity.ok(environmentComplianceService.getCompliance(cultivationId));
    }
}
