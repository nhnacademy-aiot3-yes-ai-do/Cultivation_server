package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.config.SensorCacheProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorCacheStatus;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorLatestValueService {
    private final InfluxService influxService;
    private final SensorRedisCacheService sensorRedisCacheService;
    private final SensorCacheProperties sensorCacheProperties;
    private final ConcurrentHashMap<Long, CompletableFuture<LatestSensorValueListResponse>> latestFallbacks =
            new ConcurrentHashMap<>();

    public ResponseEntity<LatestSensorValueListResponse> getLatest(Long cultivationId) {
        try {
            SensorRedisCacheService.LatestCacheReadResult cacheResult =
                    sensorRedisCacheService.findLatestWithStatus(cultivationId, freshness());
            return resolveLatestWithoutFreshCache(cultivationId, cacheResult);
        } catch (RuntimeException e) {
            log.warn("센서 최신값 Redis 조회 실패: cultivationId={}, stage=redis-latest", cultivationId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new LatestSensorValueListResponse(List.of(), LatestSensorCacheStatus.NO_DATA));
        }
    }

    private Duration freshness() {
        return Duration.ofSeconds(sensorCacheProperties.getFreshnessSeconds());
    }

    private ResponseEntity<LatestSensorValueListResponse> resolveLatestWithoutFreshCache(
            Long cultivationId, SensorRedisCacheService.LatestCacheReadResult initialCacheResult) {
        if (!initialCacheResult.points().isEmpty()) return cacheResponse(initialCacheResult);

        AtomicBoolean sourceOwner = new AtomicBoolean();
        CompletableFuture<LatestSensorValueListResponse> fallback = latestFallbacks.computeIfAbsent(
                cultivationId, ignored -> createFallback(sourceOwner));
        try {
            if (sourceOwner.get()) loadInfluxFallback(cultivationId, fallback);
            return resolveFallbackResponse(cultivationId, fallback);
        } catch (RuntimeException e) {
            log.warn("센서 최신값 fallback 실패: cultivationId={}, stage=influx-latest", cultivationId, e);
            return unavailableResponse(initialCacheResult);
        } finally {
            removeFallbackWhenOwner(cultivationId, fallback, sourceOwner);
        }
    }

    private CompletableFuture<LatestSensorValueListResponse> createFallback(AtomicBoolean sourceOwner) {
        sourceOwner.set(true);
        return new CompletableFuture<>();
    }

    private void loadInfluxFallback(Long cultivationId, CompletableFuture<LatestSensorValueListResponse> fallback) {
        try {
            LatestSensorValueListResponse source = influxService.findLatestByCultivationId(cultivationId);
            if (source == null || source.latestSensorValueResponses() == null) {
                throw new IllegalStateException("Influx latest response is null");
            }
            fallback.complete(source);
        } catch (RuntimeException e) {
            fallback.completeExceptionally(e);
        } finally {
            if (!fallback.isDone()) fallback.completeExceptionally(new IllegalStateException("Influx latest fallback failed"));
        }
    }

    private ResponseEntity<LatestSensorValueListResponse> resolveFallbackResponse(
            Long cultivationId, CompletableFuture<LatestSensorValueListResponse> fallback) {
        LatestSensorValueListResponse source = awaitFallback(fallback);
        SensorRedisCacheService.LatestCacheReadResult refreshed = refreshCache(cultivationId);
        if (refreshed != null && !refreshed.points().isEmpty()) return cacheResponse(refreshed);

        LatestSensorCacheStatus status = source.latestSensorValueResponses().isEmpty()
                ? LatestSensorCacheStatus.NO_DATA : LatestSensorCacheStatus.SOURCE_FALLBACK;
        return ResponseEntity.ok(new LatestSensorValueListResponse(source.latestSensorValueResponses(), status));
    }

    private SensorRedisCacheService.LatestCacheReadResult refreshCache(Long cultivationId) {
        try {
            return sensorRedisCacheService.findLatestWithStatus(cultivationId, freshness());
        } catch (RuntimeException e) {
            log.warn("센서 최신값 fallback 후 Redis 재확인 실패: cultivationId={}, stage=redis-latest-refresh",
                    cultivationId, e);
            return null;
        }
    }

    private ResponseEntity<LatestSensorValueListResponse> cacheResponse(
            SensorRedisCacheService.LatestCacheReadResult cacheResult) {
        return ResponseEntity.ok(new LatestSensorValueListResponse(
                cacheResult.points(), cacheResult.hasStaleValues() ? LatestSensorCacheStatus.PARTIAL : LatestSensorCacheStatus.FRESH));
    }

    private ResponseEntity<LatestSensorValueListResponse> unavailableResponse(
            SensorRedisCacheService.LatestCacheReadResult initialCacheResult) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new LatestSensorValueListResponse(List.of(),
                        initialCacheResult.hasStaleValues() ? LatestSensorCacheStatus.REDIS_PENDING : LatestSensorCacheStatus.NO_DATA));
    }

    private void removeFallbackWhenOwner(Long cultivationId,
                                         CompletableFuture<LatestSensorValueListResponse> fallback,
                                         AtomicBoolean sourceOwner) {
        if (sourceOwner.get()) latestFallbacks.remove(cultivationId, fallback);
    }

    private LatestSensorValueListResponse awaitFallback(CompletableFuture<LatestSensorValueListResponse> fallback) {
        try {
            return fallback.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Influx latest fallback interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Influx latest fallback failed", cause);
        }
    }
}
