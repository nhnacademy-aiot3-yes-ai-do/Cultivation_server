package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;

public record SensorTrendPointListResponse (
        String unit,
        List<SensorTrendPointResponse> responses
) {
}
