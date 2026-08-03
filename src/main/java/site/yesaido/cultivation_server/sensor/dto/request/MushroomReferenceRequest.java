package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MushroomReferenceRequest(
        @NotBlank
        String mushroomNameKo,
        @NotBlank
        String mushroomNameEn,
        @NotBlank
        String mushroomScientificName,
        @NotEmpty
        List<MushroomReferenceThresholdRequest> thresholds
) {
}
