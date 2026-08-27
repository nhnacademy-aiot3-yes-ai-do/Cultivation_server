package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotNull;

import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThresholdType;

import java.math.BigDecimal;

public record MushroomReferenceThresholdRequest(
        Long id,
        @NotNull
        Long sensorTypeId,
        @NotNull
        MushroomReferenceThresholdType thresholdType,
        @NotNull
        BigDecimal thresholdMin,
        @NotNull
        BigDecimal thresholdMax
) {
}
