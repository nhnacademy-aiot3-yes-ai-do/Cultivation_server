package site.yesaido.cultivation_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.service.HarvestService;

@RestController
@RequestMapping("/api/cultivations/{cultivation-id}/harvest")
@RequiredArgsConstructor
public class HarvestController {
    private final HarvestService harvestService;

    @PostMapping
    public ResponseEntity<HarvestCreateResponse> createHarvest(@PathVariable("cultivation-id") Long cultivationId,
                                                               @RequestHeader("X-User-Id") Long userId,
                                                               @Valid @RequestBody HarvestCreateRequest request) {
        HarvestCreateResponse response = harvestService.createHarvest(cultivationId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<HarvestDetailResponse> getHarvest(@PathVariable("cultivation-id") Long cultivationId,
                                                            @RequestHeader("X-User-Id") Long userId) {
        HarvestDetailResponse response = harvestService.getHarvest(cultivationId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/product-score")
    public ResponseEntity<ProductScoreUpdateResponse> updateProductScore(@PathVariable("cultivation-id") Long cultivationId,
                                                                         @RequestHeader("X-User-Id") Long userId,
                                                                         @Valid @RequestBody ProductScoreUpdateRequest request) {
        ProductScoreUpdateResponse response = harvestService.updateProductScore(cultivationId, userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
