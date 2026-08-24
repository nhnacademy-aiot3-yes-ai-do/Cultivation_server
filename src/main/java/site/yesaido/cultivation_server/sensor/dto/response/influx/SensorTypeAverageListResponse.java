package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;

public record SensorTypeAverageListResponse(
        List<SensorTypeAverageResponse> sensorTypeAverages
) {
}
