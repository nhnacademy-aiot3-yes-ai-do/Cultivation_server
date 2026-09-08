package site.yesaido.cultivation_server.cultivation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.controller.docs.CultivationMetadataControllerDocs;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationMetadataService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations")
public class CultivationMetadataController implements CultivationMetadataControllerDocs {
    private final CultivationMetadataService cultivationMetadataService;

    @Override
    @GetMapping("/metadata")
    public ResponseEntity<CultivationMetadataListResponse> getList(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(cultivationMetadataService.getList(userId));
    }

    @Override
    @GetMapping("/{cultivation-id}/metadata")
    public ResponseEntity<CultivationMetadataResponse> get(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("cultivation-id") Long cultivationId
    ) {
        return ResponseEntity.ok(cultivationMetadataService.get(userId, cultivationId, role));
    }
}
