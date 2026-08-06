package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/cultivations/{cultivation-id}/environment-compliance")
@RequiredArgsConstructor
public class EnvironmentComplianceController {
    private final EnvironmentComplianceService environmentComplianceService;

    @GetMapping
    public ResponseEntity<EnvironmentComplianceResponse> get(@PathVariable("cultivation-id") Long cultivationId) {
        return ResponseEntity.ok(environmentComplianceService.getCompliance(cultivationId));
    }

    @GetMapping("/daily")
    public ResponseEntity<EnvironmentComplianceResponse> getDaily(@PathVariable("cultivation-id") Long cultivationId,
                                                                  @RequestParam(value = "date", required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        return ResponseEntity.ok(environmentComplianceService.getDailyCompliance(cultivationId, targetDate));
    }
}
