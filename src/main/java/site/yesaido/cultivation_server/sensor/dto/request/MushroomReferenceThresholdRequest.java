package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MushroomReferenceThresholdRequest(
        Long id,
        @NotNull
        Long sensorTypeId,
        @NotNull
        BigDecimal thresholdMin,
        @NotNull
        BigDecimal thresholdMax
) {
}
