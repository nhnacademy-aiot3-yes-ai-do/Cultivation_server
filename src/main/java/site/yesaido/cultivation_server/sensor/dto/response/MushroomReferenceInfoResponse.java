package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.MushroomReference;

import java.time.LocalDateTime;
import java.util.List;

public record MushroomReferenceInfoResponse(
        long id,
        String mushroomNameKo,
        String mushroomNameEn,
        String mushroomScientificName,
        List<MushroomReferenceThresholdInfoResponse> thresholdInfoResponses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MushroomReferenceInfoResponse from(MushroomReference mushroomReference) {
        List<MushroomReferenceThresholdInfoResponse> thresholdInfoResponses = MushroomReferenceThresholdInfoResponse.from(mushroomReference.getMushroomReferenceThresholds());

        return new MushroomReferenceInfoResponse(mushroomReference.getId(), mushroomReference.getMushroomNameKo(),
                mushroomReference.getMushroomNameEn(), mushroomReference.getMushroomScientificName(),
                thresholdInfoResponses, mushroomReference.getCreatedAt(), mushroomReference.getUpdatedAt());
    }
}
