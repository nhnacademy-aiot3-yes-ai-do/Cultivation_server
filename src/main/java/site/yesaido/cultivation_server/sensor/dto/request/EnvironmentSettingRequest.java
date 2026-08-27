package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// environment_setting 테이블
public record EnvironmentSettingRequest(
        @NotNull
        @Positive
        Long sensorTypeId,

        @NotNull
        BigDecimal thresholdMin,

        @NotNull
        BigDecimal thresholdMax
) {
}
