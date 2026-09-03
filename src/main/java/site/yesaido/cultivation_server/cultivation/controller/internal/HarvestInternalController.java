package site.yesaido.cultivation_server.cultivation.controller.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.service.HarvestService;

@RestController
@RequestMapping("/api/v1/internal/cultivations/{cultivation-id}/harvest")
@RequiredArgsConstructor
public class HarvestInternalController {
    private final HarvestService harvestService;

    @PutMapping("/product-score")
    public ResponseEntity<ProductScoreUpdateResponse> updateProductScore(@PathVariable("cultivation-id") Long cultivationId,
                                                                         @Valid @RequestBody ProductScoreUpdateRequest productScoreUpdateRequest) {
        ProductScoreUpdateResponse response = harvestService.updateProductScoreInternal(cultivationId, productScoreUpdateRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
