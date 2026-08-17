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
}