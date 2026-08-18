package site.yesaido.cultivation_server.sensor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/mushroom-references")
public class MushroomReferenceManagementController {
    private final MushroomReferenceService mushroomReferenceService;

    @PostMapping
    public ResponseEntity<Void> registerMushroomReference(@Valid @RequestBody MushroomReferenceRequest request) {
        long mushroomReferenceId = mushroomReferenceService.registerMushroomReference(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(mushroomReferenceId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{mushroom-reference-id}")
    public ResponseEntity<Void> updateMushroomReference(@PathVariable("mushroom-reference-id")Long id,
                                                        @Valid @RequestBody MushroomReferenceRequest request) {
        mushroomReferenceService.updateMushroomReference(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{mushroom-reference-id}")
    public ResponseEntity<Void> deleteMushroomReference(@PathVariable("mushroom-reference-id")Long id) {
        mushroomReferenceService.deleteMushroomReference(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{mushroom-reference-id}")
    public ResponseEntity<MushroomReferenceInfoResponse> getMushroomReference(@PathVariable("mushroom-reference-id")Long id) {
        MushroomReferenceInfoResponse mushroomReferenceInfo = mushroomReferenceService.getMushroomReferenceInfo(id);
        return ResponseEntity.status(HttpStatus.OK).body(mushroomReferenceInfo);
    }

    @GetMapping
    public ResponseEntity<MushroomReferenceInfoListResponse> getAllMushroomReference() {
        MushroomReferenceInfoListResponse allMushroomReferenceInfoList = mushroomReferenceService.getAllMushroomReferenceInfoList();
        return ResponseEntity.status(HttpStatus.OK).body(allMushroomReferenceInfoList);
    }
}
