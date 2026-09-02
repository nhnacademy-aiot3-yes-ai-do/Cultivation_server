package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorCacheScheduler {
    private final CultivationSensorRepository sensorRepository;
    private final InfluxService influxService;
    private final SensorRedisCacheService cacheService;
    private final StringRedisTemplate redis;

    private static final String LOCK_KEY = "cultivation:sensor:cache:refresh-lock";
    private static final String WATERMARK_PREFIX = "cultivation:sensor:cache:watermark:";
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    @Value("${sensor-cache.history-hours:12}")
    private long historyHours;
    @Value("${sensor-cache.ttl-grace-seconds:3}")
    private long ttlGraceSeconds;
    @Value("${sensor-cache.query-overlap-seconds:60}")
    private long queryOverlapSeconds;
    @Value("${sensor-cache.lock-lease-seconds:600}")
    private long lockLeaseSeconds;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean warmedUp;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        warmedUp = refresh(Duration.ofHours(historyHours), true);
    }

    @Scheduled(fixedDelayString = "${sensor-cache.poll-interval-ms:2000}",
            initialDelayString = "${sensor-cache.poll-initial-delay-ms:10000}")
    public void poll() {
        if (!warmedUp) {
            warmedUp = refresh(Duration.ofHours(historyHours), true);
            return;
        }
        refresh(Duration.ofSeconds(queryOverlapSeconds), false);
    }

    private boolean refresh(Duration range, boolean warmup) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        String token = UUID.randomUUID().toString();
        Duration lease = Duration.ofSeconds(lockLeaseSeconds);
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(LOCK_KEY, token, lease);
        } catch (RuntimeException e) {
            running.set(false);
            log.warn("센서 캐시 분산락 획득 실패: 다음 주기에 재시도합니다.");
            return false;
        }
        if (!Boolean.TRUE.equals(acquired)) {
            running.set(false);
            return false;
        }
        boolean success = false;
        try {
            Set<CultivationStatus> statuses = Set.of(CultivationStatus.CREATED, CultivationStatus.RUNNING);
            List<Long> cultivationIds = sensorRepository.findAllForDataGeneratorSnapshot(statuses).stream()
                    .map(CultivationSensor::getCultivationId)
                    .distinct()
                    .toList();
            success = cultivationIds.stream()
                    .map(cultivationId -> refreshCultivation(cultivationId, range, warmup))
                    .allMatch(Boolean.TRUE::equals);
        } catch (Exception e) {
            log.warn("센서 Redis 캐시 갱신 실패: 원본 InfluxDB 조회는 유지됩니다.");
        } finally {
            try {
                redis.execute(new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                        List.of(LOCK_KEY), token);
            } catch (RuntimeException e) {
                log.warn("센서 캐시 분산락 해제 실패: lease 만료를 기다립니다.");
            } finally {
                running.set(false);
            }
        }
        return success;
    }

    private boolean refreshCultivation(long cultivationId, Duration range, boolean warmup) {
        try {
            Instant now = Instant.now();
            String watermarkKey = WATERMARK_PREFIX + cultivationId;
            Duration queryRange = warmup ? range : queryRange(watermarkKey, now, range);
            var points = influxService.findValuesByCultivationId(cultivationId, queryRange);
            cacheService.append(cultivationId, points,
                    Duration.ofHours(historyHours), Duration.ofSeconds(ttlGraceSeconds));
            points.stream()
                    .map(LatestSensorValueResponse::measuredAt)
                    .filter(java.util.Objects::nonNull)
                    .max(Instant::compareTo)
                    .ifPresent(latest -> redis.opsForValue().set(
                            watermarkKey,
                            latest.toString(),
                            Duration.ofHours(historyHours).plusSeconds(ttlGraceSeconds)));
            return true;
        } catch (Exception e) {
            log.warn("센서 Redis 캐시 갱신 건너뜀: cultivationId={}", cultivationId);
            return false;
        }
    }

    private Duration queryRange(String watermarkKey, Instant now, Duration fallback) {
        String watermark = redis.opsForValue().get(watermarkKey);
        if (watermark == null) {
            return fallback;
        }
        try {
            Instant lastMeasuredAt = Instant.parse(watermark);
            long seconds = Math.max(1, Duration.between(lastMeasuredAt, now).plusSeconds(queryOverlapSeconds).toSeconds());
            return Duration.ofSeconds(seconds);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
