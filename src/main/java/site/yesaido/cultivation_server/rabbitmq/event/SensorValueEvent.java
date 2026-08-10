package site.yesaido.cultivation_server.rabbitmq.event;

import java.time.LocalDateTime;

public record SensorValueEvent(
        String place,
        String location,
        String deviceModel,
        String deviceName,
        String deviceEui,
        SensorType sensorType,
        Double value,
        LocalDateTime time,
        Long cultivationId
        // String unit 추가필요
) {
}
