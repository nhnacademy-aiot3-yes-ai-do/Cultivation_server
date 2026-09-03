package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationMetadataResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationSummaryListResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationMetadataService;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;
import site.yesaido.cultivation_server.sensor.service.SensorRedisCacheService;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CultivationMetadataServiceImpl implements CultivationMetadataService {
    private final CultivationService cultivationService;
    private final CultivationSensorFacade cultivationSensorFacade;
    private final MushroomReferenceService mushroomReferenceService;
    private final InfluxService influxService;
    private final SensorRedisCacheService sensorRedisCacheService;

    @Value("${sensor-cache.freshness-seconds:3}")
    private long freshnessSeconds;

    @Override
    public CultivationMetadataResponse get(Long userId, Long cultivationId) {
        var cultivation = cultivationService.getCultivation(userId, cultivationId);
        return new CultivationMetadataResponse(
                cultivation,
                cultivationSensorFacade.findAll(userId, cultivationId),
                mushroomReferenceService.getMushroomReferenceInfo(cultivation.mushroomId()),
                influxService.findValuesByCultivationId(cultivationId, Duration.ofHours(12))
        );
    }

    @Override
    public CultivationMetadataListResponse getList(Long userId) {
        CultivationSummaryListResponse summaries = cultivationService.getCultivations(userId);
        List<Long> cultivationIds = summaries.cultivationSummaryResponses().stream()
                .map(summary -> summary.cultivationId())
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, List<LatestSensorValueResponse>> latestByCultivationId = latestValues(cultivationIds);
        List<CultivationMetadataListResponse.CultivationMetadataListItemResponse> items =
                summaries.cultivationSummaryResponses().stream()
                        .map(summary -> new CultivationMetadataListResponse.CultivationMetadataListItemResponse(
                                summary,
                                latestByCultivationId.getOrDefault(summary.cultivationId(), List.of())
                        ))
                        .toList();
        return new CultivationMetadataListResponse(items);
    }

    private String sensorKey(LatestSensorValueResponse point) {
        return point.deviceEui() + "|" + point.sensorType() + "|" + point.unit();
    }

    private Map<Long, List<LatestSensorValueResponse>> latestValues(List<Long> cultivationIds) {
        Map<Long, List<LatestSensorValueResponse>> result = new LinkedHashMap<>();
        try {
            result.putAll(sensorRedisCacheService.findLatest(cultivationIds, Duration.ofSeconds(freshnessSeconds)));
        } catch (RuntimeException exception) {
            // Redis 장애 시 Influx batch 결과로 목록을 구성한다.
        }
        try {
            Map<Long, List<LatestSensorValueResponse>> fallback =
                    influxService.findLatestByCultivationIds(cultivationIds);
            fallback.forEach((cultivationId, points) -> {
                Map<String, LatestSensorValueResponse> merged = new LinkedHashMap<>();
                result.getOrDefault(cultivationId, List.of()).forEach(point -> merged.put(sensorKey(point), point));
                points.forEach(point -> merged.putIfAbsent(sensorKey(point), point));
                result.put(cultivationId, List.copyOf(merged.values()));
            });
        } catch (RuntimeException exception) {
            // Influx 장애 시 Redis에서 확보한 fresh 값만 반환한다.
        }
        return result;
    }
}
