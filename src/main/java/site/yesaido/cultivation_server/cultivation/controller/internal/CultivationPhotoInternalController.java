package site.yesaido.cultivation_server.cultivation.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.DailyCultivationPhotoListResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationPhotoService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/internal/cultivations/photos")
@RequiredArgsConstructor
public class CultivationPhotoInternalController {
    private final CultivationPhotoService cultivationPhotoService;

    @GetMapping("/daily")
    public ResponseEntity<DailyCultivationPhotoListResponse> getDailyPhotos(@RequestParam("date")
                                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        return ResponseEntity.ok(cultivationPhotoService.getDailyPhotos(targetDate));
    }
}
