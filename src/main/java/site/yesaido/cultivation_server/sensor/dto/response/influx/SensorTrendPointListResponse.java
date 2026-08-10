package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;

public record SensorTrendPointListResponse (
        List<SensorTrendPointResponse> responses
) {
}
