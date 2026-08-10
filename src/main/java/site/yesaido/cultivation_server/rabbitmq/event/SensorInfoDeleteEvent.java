package site.yesaido.cultivation_server.rabbitmq.event;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SensorInfoDeleteEvent(
        @NotNull
        Long cultivationId,
        @NotBlank
        String deviceEui,
        @NotNull
        SensorType sensorType,
        @NotBlank
        String unit
) {
}
