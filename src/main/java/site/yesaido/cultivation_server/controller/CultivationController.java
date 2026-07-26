package site.yesaido.cultivation_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.service.CultivationService;

@RequestMapping("/api/cultivations")
@RestController
@RequiredArgsConstructor
public class CultivationController {
    private final CultivationService cultivationService;

    @PostMapping
    public ResponseEntity<CultivationCreateResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CultivationCreateRequest request
    ) {
        CultivationCreateResponse response = cultivationService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
