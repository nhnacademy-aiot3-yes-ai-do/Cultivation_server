package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorCacheScheduler {
    private enum RefreshResult {
        SUCCESS,
        FAILED,
        LOCK_LOST,
        NOT_ACQUIRED
    }

    private final CultivationSensorRepository sensorRepository;
    private final InfluxService influxService;
    private final SensorRedisCacheService cacheService;
    private final StringRedisTemplate redis;

    private static final String LOCK_KEY_PREFIX = "cultivation:sensor:cache:refresh-lock:";
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
    @Value("${sensor-cache.sensor-snapshot-cache-seconds:5}")
    private long sensorSnapshotCacheSeconds;
    @Value("${HOSTNAME:${spring.application.name:cultivation-server}}")
    private String instanceId;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
            2, runnable -> {
                Thread thread = new Thread(runnable, "sensor-cache-lock-heartbeat");
                thread.setDaemon(true);
                return thread;
            });
    private volatile boolean warmedUp;
    private volatile Instant lastReconciliationAt;
    private volatile List<Long> cachedCultivationIds = List.of();
    private volatile Instant cultivationIdsCachedAt;

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
        boolean success = refresh(reconciliation ? Duration.ofHours(historyHours) : Duration.ofSeconds(queryOverlapSeconds), reconciliation);
        if (reconciliation && success) {
            lastReconciliationAt = now;
        }
    }

    private boolean refresh(Duration range, boolean warmup) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        boolean success = false;
        try {
            List<Long> cultivationIds = cultivationIdsSnapshot();
            success = true;
            for (Long cultivationId : cultivationIds) {
                RefreshResult result = refreshCultivationWithLock(cultivationId, range, warmup);
                if (result == RefreshResult.LOCK_LOST) {
                    success = false;
                    continue;
                }
                if (result == RefreshResult.FAILED) {
                    success = false;
                }
            }
        } catch (Exception e) {
            log.warn("센서 Redis 캐시 갱신 실패: 원본 InfluxDB 조회는 유지됩니다.", e);
        } finally {
            running.set(false);
        }
        return success;
    }

    private RefreshResult refreshCultivationWithLock(long cultivationId, Duration range, boolean warmup) {
        String lockKey = LOCK_KEY_PREFIX + cultivationId;
        String token = instanceId + ":" + UUID.randomUUID();
        Duration lease = Duration.ofSeconds(lockLeaseSeconds);
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, token, lease);
            if (!Boolean.TRUE.equals(acquired)) {
                log.debug("센서 캐시 lock 경합: instanceId={}, cultivationId={}", instanceId, cultivationId);
                return RefreshResult.NOT_ACQUIRED;
            }
            log.debug("센서 캐시 lock 획득: instanceId={}, cultivationId={}", instanceId, cultivationId);
        } catch (RuntimeException e) {
            log.warn("센서 캐시 lock 획득 실패: instanceId={}, cultivationId={}", instanceId, cultivationId, e);
            return RefreshResult.FAILED;
        }

        AtomicBoolean leaseLost = new AtomicBoolean();
        long heartbeatSeconds = Math.max(1, lockLeaseSeconds / 3);
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (!renewLock(lockKey, token, lease)) {
                leaseLost.set(true);
                log.warn("센서 캐시 lock 갱신 실패: instanceId={}, cultivationId={}", instanceId, cultivationId);
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        long startedAt = System.nanoTime();
        try {
            BooleanSupplier ownership = () -> !leaseLost.get() && isLockOwned(lockKey, token);
            if (!ownership.getAsBoolean()) {
                return RefreshResult.LOCK_LOST;
            }
            boolean refreshed = refreshCultivation(cultivationId, range, warmup, ownership, lockKey, token);
            if (refreshed && warmup) {
                if (!ownership.getAsBoolean()) {
                    return RefreshResult.LOCK_LOST;
                }
                cacheService.compactCultivation(cultivationId,
                        Duration.ofHours(historyHours), Duration.ofSeconds(ttlGraceSeconds), lockKey, token);
            }
            if (!ownership.getAsBoolean()) {
                log.warn("센서 캐시 lock 소유권 상실: instanceId={}, cultivationId={}", instanceId, cultivationId);
                return RefreshResult.LOCK_LOST;
            }
            return refreshed ? RefreshResult.SUCCESS : RefreshResult.FAILED;
        } catch (RuntimeException e) {
            log.warn("센서 Redis 캐시 cultivation 처리 실패: instanceId={}, cultivationId={}",
                    instanceId, cultivationId, e);
            return RefreshResult.FAILED;
        } finally {
            heartbeat.cancel(false);
            try {
                Long unlocked = redis.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
                log.debug("센서 캐시 lock 해제: instanceId={}, cultivationId={}, result={}, elapsedMs={}",
                        instanceId, cultivationId, unlocked, elapsedMillis(startedAt));
            } catch (RuntimeException e) {
                log.warn("센서 캐시 lock 해제 실패: instanceId={}, cultivationId={}", instanceId, cultivationId, e);
            }
        }
    }

    private boolean renewLock(String lockKey, String token, Duration lease) {
        try {
            Long renewed = redis.execute(RENEW_SCRIPT,
                    List.of(lockKey), token, String.valueOf(lease.toSeconds()));
            return Long.valueOf(1L).equals(renewed);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isLockOwned(String lockKey, String token) {
        try {
            return token.equals(redis.opsForValue().get(lockKey));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private List<Long> cultivationIdsSnapshot() {
        Instant now = Instant.now();
        Instant cachedAt = cultivationIdsCachedAt;
        if (cachedAt != null
                && Duration.between(cachedAt, now).getSeconds() < Math.max(1, sensorSnapshotCacheSeconds)) {
            return cachedCultivationIds;
        }
        Set<CultivationStatus> statuses = Set.of(CultivationStatus.CREATED, CultivationStatus.RUNNING);
        List<Long> ids = sensorRepository.findAllForDataGeneratorSnapshot(statuses).stream()
                .map(CultivationSensor::getCultivationId)
                .distinct()
                .toList();
        cachedCultivationIds = ids;
        cultivationIdsCachedAt = now;
        return ids;
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    @PreDestroy
    void shutdownHeartbeatExecutor() {
        heartbeatExecutor.shutdownNow();
    }

    private boolean refreshCultivation(
            long cultivationId, Duration range, boolean warmup, BooleanSupplier ownership,
            String lockKey, String token) {
        try {
            Instant now = Instant.now();
            String watermarkKey = WATERMARK_PREFIX + cultivationId;
            Duration queryRange = warmup ? range : queryRange(watermarkKey, now, Duration.ofHours(historyHours));
            queryRange = queryRange.compareTo(Duration.ofHours(historyHours)) > 0
                    ? Duration.ofHours(historyHours)
                    : queryRange;
            var points = influxService.findValuesByCultivationId(cultivationId, queryRange);
            if (!ownership.getAsBoolean()) {
                return false;
            }
            boolean appended = false;
            for (int attempt = 0; attempt < 3 && ownership.getAsBoolean(); attempt++) {
                appended = cacheService.appendWithLock(cultivationId, points,
                        Duration.ofHours(historyHours), Duration.ofSeconds(ttlGraceSeconds),
                        lockKey, token);
                if (appended) {
                    break;
                }
            }
            if (!appended) {
                return false;
            }
            if (!ownership.getAsBoolean()) {
                return false;
            }
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
            log.warn("센서 Redis 캐시 갱신 건너뜀: instanceId={}, cultivationId={}", instanceId, cultivationId, e);
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
