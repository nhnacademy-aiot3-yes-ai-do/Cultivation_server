package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;
import java.util.Map;

public record LatestSensorValueBatchResponse(
        Map<Long, List<LatestSensorValueResponse>> latestSensorValuesByCultivationId
) {
    public LatestSensorValueBatchResponse {
        latestSensorValuesByCultivationId = latestSensorValuesByCultivationId == null
                ? Map.of()
                : Map.copyOf(latestSensorValuesByCultivationId);
    }
}
