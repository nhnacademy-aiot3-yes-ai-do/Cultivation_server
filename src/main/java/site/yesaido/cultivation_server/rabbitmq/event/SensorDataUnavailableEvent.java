package site.yesaido.cultivation_server.rabbitmq.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SensorDataUnavailableEvent(
        UUID eventId,
        long cultivationId,
        String deviceName,
        String message,
        OffsetDateTime occurredAt
) {
}
