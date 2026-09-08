package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.controller.docs.EnvironmentComplianceControllerDocs;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/cultivations/{cultivation-id}/environment-compliance")
@RequiredArgsConstructor
public class EnvironmentComplianceController implements EnvironmentComplianceControllerDocs {
    private final EnvironmentComplianceService environmentComplianceService;
    private final CultivationMemberService cultivationMemberService;

    @Override
    @GetMapping
    public ResponseEntity<EnvironmentComplianceResponse> get(@PathVariable("cultivation-id") Long cultivationId,
                                                             @RequestHeader("X-User-Id") Long userId,
                                                             @RequestHeader(value = "X-User-Role", required = false) String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        return ResponseEntity.ok(environmentComplianceService.getCompliance(cultivationId));
    }

    @Override
    @GetMapping("/daily")
    public ResponseEntity<EnvironmentComplianceResponse> getDaily(@PathVariable("cultivation-id") Long cultivationId,
                                                                  @RequestParam(value = "date", required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                  @RequestHeader("X-User-Id") Long userId,
                                                                  @RequestHeader(value = "X-User-Role", required = false) String role) {
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        return ResponseEntity.ok(environmentComplianceService.getDailyCompliance(cultivationId, targetDate));
    }

    @Override
    @GetMapping("/period")
    public ResponseEntity<EnvironmentComplianceResponse> getPeriod(@PathVariable("cultivation-id") Long cultivationId,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                   @RequestHeader("X-User-Id") Long userId,
                                                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        return ResponseEntity.ok(environmentComplianceService.getComplianceForPeriod(cultivationId, startDate, endDate));
    }
}
