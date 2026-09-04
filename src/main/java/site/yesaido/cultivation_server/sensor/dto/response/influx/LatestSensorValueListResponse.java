package site.yesaido.cultivation_server.sensor.dto.response.influx;

import java.util.List;

public record LatestSensorValueListResponse(
        List<LatestSensorValueResponse> latestSensorValueResponses,
        LatestSensorCacheStatus cacheStatus
) {
    public LatestSensorValueListResponse(List<LatestSensorValueResponse> responses) {
        this(responses, responses == null || responses.isEmpty()
                ? LatestSensorCacheStatus.NO_DATA
                : LatestSensorCacheStatus.SOURCE_FALLBACK);
    }
}
