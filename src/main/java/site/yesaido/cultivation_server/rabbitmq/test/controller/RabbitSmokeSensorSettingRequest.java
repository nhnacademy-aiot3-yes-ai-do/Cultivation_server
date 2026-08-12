package site.yesaido.cultivation_server.rabbitmq.test.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RabbitSmokeSensorSettingRequest(
        @NotBlank
        String sensorType,

        @NotBlank
        String unit,

        @NotNull
        BigDecimal thresholdMin,

        @NotNull
        BigDecimal thresholdMax
) {
    public RabbitSmokeSensorSettingRequest {
        if (thresholdMin != null
                && thresholdMax != null
                && thresholdMin.compareTo(thresholdMax) >= 0) {
            throw new IllegalArgumentException(
                    "thresholdMin은 thresholdMax보다 작아야 합니다."
            );
        }
    }
}