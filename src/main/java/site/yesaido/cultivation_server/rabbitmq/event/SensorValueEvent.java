package site.yesaido.cultivation_server.rabbitmq.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SensorValueEvent(
        String place,
        String location,
        String deviceModel,
        String deviceName,
        String deviceEui,
        String sensorType,
        String unit,
        BigDecimal value,
        OffsetDateTime time,
        Long cultivationId
) {
}
