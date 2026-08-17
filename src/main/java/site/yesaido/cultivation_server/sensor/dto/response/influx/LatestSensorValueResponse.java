package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.math.BigDecimal;
import java.time.Instant;

public record LatestSensorValueResponse(
        Long cultivationId,
        String sensorType,
        String unit,
        BigDecimal value,
        Instant measuredAt,
        String deviceEui,
        String deviceModel,
        String deviceName,
        String location,
        String place
) {
}
