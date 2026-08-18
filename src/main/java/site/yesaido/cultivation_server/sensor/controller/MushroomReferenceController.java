package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/mushroom-references")
public class MushroomReferenceController {
    private final MushroomReferenceService mushroomReferenceService;

    @GetMapping
    public ResponseEntity<MushroomReferenceInfoListResponse> getAllMushroomReference() {
        MushroomReferenceInfoListResponse allMushroomReferenceInfoList = mushroomReferenceService.getAllMushroomReferenceInfoList();
        return ResponseEntity.status(HttpStatus.OK).body(allMushroomReferenceInfoList);
    }
}
