package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;

public record SensorTrendPointListResponse (
        long cultivationId,
        String deviceEui,
        String sensorType,
        String unit,
        List<SensorTrendPointResponse> responses
) {
}
