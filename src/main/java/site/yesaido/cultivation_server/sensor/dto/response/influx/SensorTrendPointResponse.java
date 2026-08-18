package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.math.BigDecimal;
import java.time.Instant;

public record SensorTrendPointResponse(
        Instant measuredAt,
        BigDecimal value
) {
}
