package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.math.BigDecimal;

public record SensorTypeAverageResponse(
        Long cultivationId,
        String sensorType,
        String unit,
        BigDecimal averageValue
) {
}
