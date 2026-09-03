package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = redisScript("scripts/redis/unlock.lua");
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = redisScript("scripts/redis/renew.lua");

    private static DefaultRedisScript<Long> redisScript(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }

    @Value("${sensor-cache.history-hours:12}")
    private long historyHours;
    @Value("${sensor-cache.ttl-grace-seconds:3}")
    private long ttlGraceSeconds;
    @Value("${sensor-cache.query-overlap-seconds:60}")
    private long queryOverlapSeconds;
    @Value("${sensor-cache.lock-lease-seconds:600}")
    private long lockLeaseSeconds;
    @Value("${sensor-cache.reconciliation-interval-seconds:300}")
    private long reconciliationIntervalSeconds;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean warmedUp;
    private volatile Instant lastReconciliationAt;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        boolean success = refresh(Duration.ofHours(historyHours), true);
        if (success) {
            lastReconciliationAt = Instant.now();
        }
        warmedUp = success;
    }

    @Scheduled(fixedDelayString = "${sensor-cache.poll-interval-ms:2000}",
            initialDelayString = "${sensor-cache.poll-initial-delay-ms:10000}")
    public void poll() {
        if (!warmedUp) {
            boolean success = refresh(Duration.ofHours(historyHours), true);
            if (success) {
                lastReconciliationAt = Instant.now();
            }
            warmedUp = success;
            return;
        }
        Instant now = Instant.now();
        boolean reconciliation = lastReconciliationAt == null
                || Duration.between(lastReconciliationAt, now).getSeconds() >= reconciliationIntervalSeconds;
        if (reconciliation) {
            lastReconciliationAt = now;
        }
        refresh(reconciliation ? Duration.ofHours(historyHours) : Duration.ofSeconds(queryOverlapSeconds), reconciliation);
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
            success = true;
            for (Long cultivationId : cultivationIds) {
                if (!renewLock(token, lease) || !refreshCultivation(cultivationId, range, warmup)) {
                    success = false;
                }
            }
        } catch (Exception e) {
            log.warn("센서 Redis 캐시 갱신 실패: 원본 InfluxDB 조회는 유지됩니다.");
        } finally {
            try {
                redis.execute(UNLOCK_SCRIPT,
                        List.of(LOCK_KEY), token);
            } catch (RuntimeException e) {
                log.warn("센서 캐시 분산락 해제 실패: lease 만료를 기다립니다.");
            } finally {
                running.set(false);
            }
        }
        return success;
    }

    private boolean renewLock(String token, Duration lease) {
        try {
            Long renewed = redis.execute(RENEW_SCRIPT,
                    List.of(LOCK_KEY), token, String.valueOf(lease.toSeconds()));
            return Long.valueOf(1L).equals(renewed);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean refreshCultivation(long cultivationId, Duration range, boolean warmup) {
        try {
            Instant now = Instant.now();
            String watermarkKey = WATERMARK_PREFIX + cultivationId;
            Duration queryRange = warmup ? range : queryRange(watermarkKey, now, Duration.ofHours(historyHours));
            queryRange = queryRange.compareTo(Duration.ofHours(historyHours)) > 0
                    ? Duration.ofHours(historyHours)
                    : queryRange;
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
