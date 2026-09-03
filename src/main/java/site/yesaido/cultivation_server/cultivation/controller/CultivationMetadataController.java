package site.yesaido.cultivation_server.cultivation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationMetadataService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations")
public class CultivationMetadataController {
    private final CultivationMetadataService cultivationMetadataService;

    @GetMapping("/metadata")
    public ResponseEntity<CultivationMetadataListResponse> getList(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(cultivationMetadataService.getList(userId));
    }

    @GetMapping("/{cultivation-id}/metadata")
    public ResponseEntity<CultivationMetadataResponse> get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("cultivation-id") Long cultivationId
    ) {
        return ResponseEntity.ok(cultivationMetadataService.get(userId, cultivationId));
    }
}
