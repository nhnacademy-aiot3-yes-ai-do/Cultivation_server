package site.yesaido.cultivation_server.rabbitmq.event;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SensorInfoDeleteEvent(
        @NotNull
        Long cultivationId,
        @NotBlank
        String deviceEui,
        @NotBlank
        String sensorType,
        @NotBlank
        String unit,
        //OffsetDateTime.now(ZoneOffset.UTC)
        @NotNull
        OffsetDateTime occurredAt
) {
}