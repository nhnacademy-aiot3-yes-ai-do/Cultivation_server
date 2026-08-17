package site.yesaido.cultivation_server.sensor.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record LatestSensorReadingResponse(
        Long sensorId,
        Long sensorTypeId,
        BigDecimal value,
        Instant measuredAt
) {
}
