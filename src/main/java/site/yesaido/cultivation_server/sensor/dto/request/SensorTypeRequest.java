package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SensorTypeRequest(
        @NotBlank
        String type,
        @NotBlank
        String valueUnit
) {
}
