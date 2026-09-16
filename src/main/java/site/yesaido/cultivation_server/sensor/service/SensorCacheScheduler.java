package site.yesaido.cultivation_server.sensor.service;

import jakarta.annotation.PreDestroy;
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
import site.yesaido.cultivation_server.config.SensorCacheProperties;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private final SensorConnectionService sensorConnectionService;

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

    private final SensorCacheProperties sensorCacheProperties;
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
    private final ConcurrentHashMap<Long, Instant> lastReconciliationByCultivation = new ConcurrentHashMap<>();
    private final AtomicReference<List<Long>> cachedCultivationIds = new AtomicReference<>(List.of());
    private final AtomicReference<Instant> cultivationIdsCachedAt = new AtomicReference<>();

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        warmedUp = refresh(true);
    }

    @Scheduled(fixedDelayString = "${sensor-cache.poll-interval-ms:2000}",
            initialDelayString = "${sensor-cache.poll-initial-delay-ms:10000}")
    public void poll() {
        if (!warmedUp) {
            warmedUp = refresh(true);
            return;
        }
        refresh(false);
    }

    /**
     * 재검증(12시간 전체 재조회) 대상 여부를 재배지별로 판단합니다.
     * 인스턴스 단위로 한꺼번에 재검증하면 그 순간 모든 재배지가 동시에 풀스캔을 돌면서
     * 한 바퀴 전체가 수십 초~수 분 지연되는 현상이 있어, 재배지마다 마지막 재검증 시각을
     * 따로 추적해 재검증 시점을 자연스럽게 분산시킵니다.
     */
    private boolean isReconciliationDue(long cultivationId, Instant now) {
        Instant last = lastReconciliationByCultivation.get(cultivationId);
        return last == null
                || Duration.between(last, now).getSeconds() >= sensorCacheProperties.getReconciliationIntervalSeconds();
    }

    private boolean refresh(boolean forceFullRange) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        boolean success = false;
        try {
            List<Long> cultivationIds = cultivationIdsSnapshot();
            success = true;
            int[] reconciliationBudget = {MAX_RECONCILIATIONS_PER_CYCLE};
            for (Long cultivationId : cultivationIds) {
                if (!refreshOneCultivation(cultivationId, forceFullRange, reconciliationBudget)) {
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

    /**
     * 한 바퀴(poll 1회)당 풀스캔으로 승격시키는 재배지 수의 상한.
     * 재배지별로 재검증 시각을 따로 추적해도, 그 시각들은 애초에 warmUp 때
     * 순차 처리 지연만큼씩 벌어져 기록된다 — 그 간격이 마침 재검증(풀스캔) 1건의
     * 처리 시간과 비슷하면, 하나가 늦어져 다음 재배지 차례로 넘어가는 순간
     * 그 다음 재배지도 이미 기한을 넘겨버리는 연쇄가 발생해 결국 한 바퀴에
     * 전부 몰릴 수 있다. 바퀴당 승격 개수를 제한해 이 연쇄를 끊는다.
     */
    private static final int MAX_RECONCILIATIONS_PER_CYCLE = 1;

    private boolean refreshOneCultivation(Long cultivationId, boolean forceFullRange, int[] reconciliationBudget) {
        Instant now = Instant.now();
        boolean dueForReconciliation = isReconciliationDue(cultivationId, now);
        boolean reconciliation = forceFullRange || (dueForReconciliation && reconciliationBudget[0] > 0);
        if (reconciliation && !forceFullRange) {
            reconciliationBudget[0]--;
        }
        Duration range = reconciliation
                ? Duration.ofHours(sensorCacheProperties.getHistoryHours())
                : Duration.ofSeconds(sensorCacheProperties.getQueryOverlapSeconds());
        RefreshResult result = refreshCultivationWithLock(cultivationId, range, reconciliation);
        if (reconciliation && result == RefreshResult.SUCCESS) {
            lastReconciliationByCultivation.put(cultivationId, now);
        }
        return result != RefreshResult.LOCK_LOST && result != RefreshResult.FAILED;
    }

    private RefreshResult refreshCultivationWithLock(long cultivationId, Duration range, boolean warmup) {
        String lockKey = LOCK_KEY_PREFIX + cultivationId;
        String token = instanceId + ":" + UUID.randomUUID();
        Duration lease = Duration.ofSeconds(sensorCacheProperties.getLockLeaseSeconds());
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
        long heartbeatSeconds = Math.max(1, sensorCacheProperties.getLockLeaseSeconds() / 3);
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
                        Duration.ofHours(sensorCacheProperties.getHistoryHours()), Duration.ofSeconds(sensorCacheProperties.getTtlGraceSeconds()), lockKey, token);
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
        Instant cachedAt = cultivationIdsCachedAt.get();
        if (cachedAt != null
                && Duration.between(cachedAt, now).getSeconds() < Math.max(1, sensorCacheProperties.getSensorSnapshotCacheSeconds())) {
            return cachedCultivationIds.get();
        }
        Set<CultivationStatus> statuses = Set.of(CultivationStatus.CREATED, CultivationStatus.RUNNING);
        List<Long> ids = sensorRepository.findAllForDataGeneratorSnapshot(statuses).stream()
                .map(CultivationSensor::getCultivationId)
                .distinct()
                .toList();
        cachedCultivationIds.set(List.copyOf(ids));
        cultivationIdsCachedAt.set(now);
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
            Duration queryRange = warmup ? range : queryRange(watermarkKey, now, Duration.ofHours(sensorCacheProperties.getHistoryHours()));
            queryRange = queryRange.compareTo(Duration.ofHours(sensorCacheProperties.getHistoryHours())) > 0
                    ? Duration.ofHours(sensorCacheProperties.getHistoryHours())
                    : queryRange;
            var points = influxService.findValuesByCultivationId(cultivationId, queryRange);
            if (!ownership.getAsBoolean()) {
                return false;
            }
            boolean appended = false;
            for (int attempt = 0; attempt < 3 && ownership.getAsBoolean(); attempt++) {
                appended = cacheService.appendWithLock(cultivationId, points,
                        Duration.ofHours(sensorCacheProperties.getHistoryHours()), Duration.ofSeconds(sensorCacheProperties.getTtlGraceSeconds()),
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

            // 기존 조회 결과로 센서 연결 상태를 갱신합니다.
            sensorConnectionService.synchronize(cultivationId, points, now);

            // 상태 갱신 중 락을 잃었다면 워터마크를 진행하지 않습니다.
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
                            Duration.ofHours(sensorCacheProperties.getHistoryHours()).plusSeconds(sensorCacheProperties.getTtlGraceSeconds())));
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
            long seconds = Math.max(1, Duration.between(lastMeasuredAt, now).plusSeconds(sensorCacheProperties.getQueryOverlapSeconds()).toSeconds());
            return Duration.ofSeconds(seconds);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
