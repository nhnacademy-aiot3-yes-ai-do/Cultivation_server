package site.yesaido.cultivation_server.sensor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorCacheStatus;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.service.SensorRedisCacheService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/cultivations/{cultivation-id}/sensor-values")
public class SensorValueController {

    private final InfluxService influxService;
    private final SensorRedisCacheService sensorRedisCacheService;
    private final CultivationMemberService cultivationMemberService;
    private final ConcurrentHashMap<Long, CompletableFuture<LatestSensorValueListResponse>> latestFallbacks =
            new ConcurrentHashMap<>();

    @Value("${sensor-cache.freshness-seconds:3}")
    private long freshnessSeconds;

    @GetMapping("/trend")
    public ResponseEntity<SensorTrendPointListResponse> getTrend(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestParam(name = "device-eui", required = true) String deviceEui,
            @RequestParam(name = "sensor-type", required = true) String sensorType,
            @RequestParam(name = "unit", required = true) String unit,
            @RequestHeader(name = "X-User-Id") Long userId
    ) {
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        SensorTrendPointListResponse trend;
        try {
            trend = sensorRedisCacheService.findTrend(cultivationId, deviceEui, sensorType, unit);
        } catch (RuntimeException e) {
            log.warn("센서 trend Redis 조회 실패: cultivationId={}, stage=redis-trend", cultivationId, e);
            trend = null;
        }
        if (trend == null) {
            trend = influxService.findTrend(cultivationId, deviceEui, sensorType, unit);
        }
        return ResponseEntity.ok(trend);
    }

    @GetMapping
    public ResponseEntity<LatestSensorValueListResponse> getLatest(@PathVariable("cultivation-id") Long cultivationId,
                                                                   @RequestHeader("X-User-Id") Long userId,
                                                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        try {
            var cacheResult = sensorRedisCacheService.findLatestWithStatus(cultivationId,
                    java.time.Duration.ofSeconds(freshnessSeconds));
            return resolveLatestWithoutFreshCache(cultivationId, cacheResult);
        } catch (RuntimeException e) {
            log.warn("센서 최신값 Redis 조회 실패: cultivationId={}, stage=redis-latest", cultivationId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new LatestSensorValueListResponse(List.of(),
                            LatestSensorCacheStatus.NO_DATA));
        }
    }

    private ResponseEntity<LatestSensorValueListResponse> resolveLatestWithoutFreshCache(
            Long cultivationId, SensorRedisCacheService.LatestCacheReadResult initialCacheResult) {
        if (!initialCacheResult.points().isEmpty()) {
            return ResponseEntity.ok(new LatestSensorValueListResponse(
                    initialCacheResult.points(), initialCacheResult.hasStaleValues()
                            ? LatestSensorCacheStatus.PARTIAL
                            : LatestSensorCacheStatus.FRESH));
        }

        AtomicBoolean sourceOwner = new AtomicBoolean();
        CompletableFuture<LatestSensorValueListResponse> fallback = latestFallbacks.computeIfAbsent(
                cultivationId, ignored -> {
                    sourceOwner.set(true);
                    return new CompletableFuture<>();
                });
        try {
            if (sourceOwner.get()) {
                try {
                    LatestSensorValueListResponse source = influxService.findLatestByCultivationId(cultivationId);
                    if (source == null || source.latestSensorValueResponses() == null) {
                        throw new IllegalStateException("Influx latest response is null");
                    }
                    fallback.complete(source);
                } catch (Error e) {
                    fallback.completeExceptionally(e);
                    throw e;
                } catch (RuntimeException e) {
                    fallback.completeExceptionally(e);
                }
            }
            LatestSensorValueListResponse source = awaitFallback(fallback);
            try {
                var refreshedCache = sensorRedisCacheService.findLatestWithStatus(cultivationId,
                        java.time.Duration.ofSeconds(freshnessSeconds));
                if (!refreshedCache.points().isEmpty()) {
                    return ResponseEntity.ok(new LatestSensorValueListResponse(
                            refreshedCache.points(), refreshedCache.hasStaleValues()
                                    ? LatestSensorCacheStatus.PARTIAL
                                    : LatestSensorCacheStatus.FRESH));
                }
            } catch (RuntimeException e) {
                log.warn("센서 최신값 fallback 후 Redis 재확인 실패: cultivationId={}, stage=redis-latest-refresh",
                        cultivationId, e);
            }
            LatestSensorCacheStatus status = source.latestSensorValueResponses().isEmpty()
                    ? LatestSensorCacheStatus.NO_DATA
                    : LatestSensorCacheStatus.SOURCE_FALLBACK;
            return ResponseEntity.ok(new LatestSensorValueListResponse(
                    source.latestSensorValueResponses(), status));
        } catch (RuntimeException e) {
            log.warn("센서 최신값 fallback 실패: cultivationId={}, stage=influx-latest", cultivationId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new LatestSensorValueListResponse(List.of(),
                            initialCacheResult.hasStaleValues()
                                    ? LatestSensorCacheStatus.REDIS_PENDING
                                    : LatestSensorCacheStatus.NO_DATA));
        } finally {
            if (sourceOwner.get()) {
                latestFallbacks.remove(cultivationId, fallback);
            }
        }
    }

    private LatestSensorValueListResponse awaitFallback(
            CompletableFuture<LatestSensorValueListResponse> fallback) {
        try {
            return fallback.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Influx latest fallback interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Influx latest fallback failed", cause);
        }
    }

    @GetMapping("/average")
    public ResponseEntity<SensorTypeAverageListResponse> getAverage(
            @PathVariable("cultivation-id") Long cultivationId,
            @RequestHeader("X-User-Id") Long userId
    ){
        cultivationMemberService.existCultivationMember(cultivationId, userId);
        List<SensorTypeAverageResponse> average = influxService.findAverageByCultivationId(cultivationId);
        return ResponseEntity.ok(new SensorTypeAverageListResponse(average));
    }
}
