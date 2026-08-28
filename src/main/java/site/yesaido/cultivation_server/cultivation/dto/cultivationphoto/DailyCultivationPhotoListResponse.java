package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import java.time.LocalDate;
import java.util.List;

public record DailyCultivationPhotoListResponse(
        LocalDate targetDate,
        List<DailyCultivationPhotoResponse> photos
) {
}
