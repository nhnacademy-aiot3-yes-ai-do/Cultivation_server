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
                historyValues(cultivationId, Duration.ofHours(12))
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
        Map<Long, List<LatestSensorValueResponse>> trendByCultivationId = recentHistories(cultivationIds);
        List<CultivationMetadataListResponse.CultivationMetadataListItemResponse> items =
                summaries.cultivationSummaryResponses().stream()
                        .map(summary -> new CultivationMetadataListResponse.CultivationMetadataListItemResponse(
                                summary,
                                latestByCultivationId.getOrDefault(summary.cultivationId(), List.of()),
                                trendByCultivationId.getOrDefault(summary.cultivationId(), List.of())
                        ))
                        .toList();
        return new CultivationMetadataListResponse(items);
    }

    private List<LatestSensorValueResponse> historyValues(long cultivationId, Duration range) {
        try {
            List<LatestSensorValueResponse> cached = sensorRedisCacheService.findHistory(cultivationId, range);
            if (!cached.isEmpty()) return cached;
        } catch (RuntimeException exception) {
            // Redis 장애 시 원본 Influx 조회로 복구한다.
        }
        return influxService.findAveragedValuesByCultivationId(cultivationId, range);
    }

    private Map<Long, List<LatestSensorValueResponse>> recentHistories(List<Long> cultivationIds) {
        Map<Long, List<LatestSensorValueResponse>> result;
        try {
            result = new LinkedHashMap<>(sensorRedisCacheService.findHistory(cultivationIds, Duration.ofHours(1)));
        } catch (RuntimeException exception) {
            result = new LinkedHashMap<>();
        }
        for (Long cultivationId : cultivationIds) {
            if (result.getOrDefault(cultivationId, List.of()).isEmpty()) {
                try {
                    result.put(cultivationId, historyValues(cultivationId, Duration.ofHours(1)));
                } catch (RuntimeException exception) {
                    result.put(cultivationId, List.of());
                }
            }
        }
        return result;
    }

    private Map<Long, List<LatestSensorValueResponse>> latestValues(List<Long> cultivationIds) {
        try {
            return sensorRedisCacheService.findLatest(cultivationIds, Duration.ofSeconds(freshnessSeconds));
        } catch (RuntimeException exception) {
            return cultivationIds.stream().collect(java.util.stream.Collectors.toMap(
                    cultivationId -> cultivationId,
                    ignored -> List.of(),
                    (left, right) -> left,
                    LinkedHashMap::new));
        }
    }
}
