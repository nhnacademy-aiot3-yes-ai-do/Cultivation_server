package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSensorEuiProjection;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SensorLatestBatchService {
    private final CultivationSensorRepository cultivationSensorRepository;
    private final SensorRedisCacheService sensorRedisCacheService;

    public Map<Long, List<LatestSensorValueResponse>> findLatestForUser(
            Long userId,
            Duration freshness
    ) {
        Map<Long, Set<String>> sensorEuisByCultivationId = new LinkedHashMap<>();
        for (CultivationSensorEuiProjection projection
                : cultivationSensorRepository.findAllAccessibleSensorEuis(userId)) {
            if (projection.getCultivationId() != null && projection.getDeviceEui() != null) {
                sensorEuisByCultivationId
                        .computeIfAbsent(projection.getCultivationId(), ignored -> new LinkedHashSet<>())
                        .add(projection.getDeviceEui());
            }
        }
        return sensorRedisCacheService.findLatestBySensorEuis(sensorEuisByCultivationId, freshness);
    }
}
