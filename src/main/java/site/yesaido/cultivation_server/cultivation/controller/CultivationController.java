package site.yesaido.cultivation_server.cultivation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.*;
import site.yesaido.cultivation_server.cultivation.service.CultivationCreationFacade;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.sensor.service.CultivationModeFacade;

@RequestMapping("/api/v1/cultivations")
@RestController
@RequiredArgsConstructor
public class CultivationController {
    private final CultivationService cultivationService;
    private final CultivationCreationFacade cultivationCreationFacade;
    private final CultivationModeFacade cultivationModeFacade;

    @PostMapping
    public ResponseEntity<CultivationCreateResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CultivationCreateRequest request
    ) {
        CultivationCreateResponse response = cultivationCreationFacade.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CultivationSummaryListResponse> getCultivations(@RequestHeader("X-User-Id") Long userId) {
        CultivationSummaryListResponse response = cultivationService.getCultivations(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{cultivation-id}")
    public ResponseEntity<CultivationDetailResponse> getCultivation(@RequestHeader("X-User-Id") Long userId,
                                                                    @RequestHeader(value = "X-User-Role", required = false) String role,
                                                                    @PathVariable("cultivation-id") Long cultivationId
                                                                    ) {
        CultivationDetailResponse response = cultivationService.getCultivation(userId, cultivationId, role);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 재배 종료
    @PutMapping("/{cultivation-id}/finish")
    public ResponseEntity<CultivationFinishResponse> finish(@RequestHeader("X-User-Id") Long userId,
                                                            @PathVariable("cultivation-id") Long cultivationId) {
        CultivationFinishResponse response = cultivationService.finish(cultivationId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 이력 조회
    @GetMapping("/history")
    public ResponseEntity<CultivationHistoryPageResponse> getHistory(@RequestHeader("X-User-Id") Long userId,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        Page<CultivationHistoryResponse> response = cultivationService.getHistory(userId, pageable);
        return ResponseEntity.ok(CultivationHistoryPageResponse.from(response));
    }

    // 재배 삭제
    @DeleteMapping("/{cultivation-id}")
    public ResponseEntity<Void> deleteCultivation(@RequestHeader("X-User-Id") Long userId,
                                                  @RequestHeader(value = "X-User-Role", required = false) String role,
                                                  @PathVariable("cultivation-id") Long cultivationId) {
        cultivationService.delete(cultivationId, userId, role);
        return ResponseEntity.noContent().build();
    }

    // 수확 모드로 전환
    @PutMapping("/{cultivation-id}/harvest-mode")
    public ResponseEntity<CultivationModeChangeResponse> switchToHarvestMode(@RequestHeader("X-User-Id") Long userId,
                                                                             @PathVariable("cultivation-id") Long cultivationId) {
        CultivationModeChangeResponse response = cultivationModeFacade.switchToHarvestMode(cultivationId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
