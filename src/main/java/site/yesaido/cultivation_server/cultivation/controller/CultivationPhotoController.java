package site.yesaido.cultivation_server.cultivation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;

@RestController
@RequestMapping("/api/v1/cultivations/{cultivation-id}/photos")
@RequiredArgsConstructor
public class CultivationPhotoController {
    private final CultivationPhotoService cultivationPhotoService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PhotoUploadResponse> uploadPhoto(@PathVariable("cultivation-id") Long cultivationId,
                                                           @RequestHeader("X-User-Id") Long userId,
                                                           @RequestParam("file") MultipartFile file) {
        PhotoUploadResponse response = cultivationPhotoService.uploadPhoto(cultivationId, userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PhotoUploadListResponse> getPhotos(@PathVariable("cultivation-id") Long cultivationId,
                                                             @RequestHeader("X-User-Id") Long userId,
                                                             @RequestHeader(value = "X-User-Role", required = false) String role) {
        PhotoUploadListResponse response = cultivationPhotoService.getPhotos(cultivationId, userId, role);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{photo-id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable("cultivation-id") Long cultivationId,
                                            @PathVariable("photo-id") Long photoId,
                                            @RequestHeader("X-User-Id") Long userId) {
        cultivationPhotoService.deletePhoto(cultivationId, userId, photoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
