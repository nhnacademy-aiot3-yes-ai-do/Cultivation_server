package site.yesaido.cultivation_server.cultivation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;
import site.yesaido.cultivation_server.cultivation.service.MushGuideService;

@RestController
@RequestMapping("/api/mushrooms")
@RequiredArgsConstructor
public class MushGuideController {
    private final MushGuideService mushGuideService;

    @GetMapping("/{mushroom-id}/guide")
    public ResponseEntity<MushGuideResponse> getMushroomGuide(@PathVariable("mushroom-id") Long mushroomId) {
        MushGuideResponse response = mushGuideService.getMushroomGuide(mushroomId);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
}
