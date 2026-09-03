package site.yesaido.cultivation_server.cultivation.dto.cultivationphoto;

import java.time.OffsetDateTime;

public record DailyCultivationPhotoResponse(
        Long cultivationId,
        Long photoId,
        String presignedUrl,
        OffsetDateTime expiresAt
) {
}
