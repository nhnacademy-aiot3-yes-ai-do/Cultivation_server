package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorTrendService {
    private final SensorRedisCacheService sensorRedisCacheService;
    private final InfluxService influxService;

    public SensorTrendPointListResponse findTrend(Long cultivationId, String deviceEui,
                                                   String sensorType, String unit) {
        try {
            SensorTrendPointListResponse trend = sensorRedisCacheService.findTrend(
                    cultivationId, deviceEui, sensorType, unit);
            if (trend != null) {
                return trend;
            }
        } catch (RuntimeException e) {
            log.warn("센서 trend Redis 조회 실패: cultivationId={}, stage=redis-trend", cultivationId, e);
        }
        return influxService.findTrend(cultivationId, deviceEui, sensorType, unit);
    }
}
