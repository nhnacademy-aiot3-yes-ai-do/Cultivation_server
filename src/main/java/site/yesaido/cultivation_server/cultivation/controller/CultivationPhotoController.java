package site.yesaido.cultivation_server.cultivation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.PhotoUploadResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;

import java.util.List;

@RestController
@RequestMapping("/api/cultivations/{cultivation-id}/photos")
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
    public ResponseEntity<List<PhotoUploadResponse>> getPhotos(@PathVariable("cultivation-id") Long cultivationId,
                                                               @RequestHeader("X-User-Id") Long userId) {
        List<PhotoUploadResponse> response = cultivationPhotoService.getPhotos(cultivationId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{photo-id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable("cultivation-id") Long cultivationId,
                                            @PathVariable("photo-id") Long photoId,
                                            @RequestHeader("X-User-Id") Long userId) {
        cultivationPhotoService.deletePhoto(cultivationId, photoId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
