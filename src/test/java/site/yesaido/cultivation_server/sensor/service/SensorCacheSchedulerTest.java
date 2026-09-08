package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
class SensorCacheSchedulerTest {
    @Mock
    private CultivationSensorRepository sensorRepository;
    @Mock
    private InfluxService influxService;
    @Mock
    private SensorRedisCacheService cacheService;
    @Mock
    private SensorConnectionService sensorConnectionService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    private final Map<String, String> locks = new ConcurrentHashMap<>();

    private SensorCacheScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SensorCacheScheduler(sensorRepository, influxService, cacheService, redis, sensorConnectionService);
        ReflectionTestUtils.setField(scheduler, "historyHours", 12L);
        ReflectionTestUtils.setField(scheduler, "ttlGraceSeconds", 3L);
        ReflectionTestUtils.setField(scheduler, "queryOverlapSeconds", 60L);
        ReflectionTestUtils.setField(scheduler, "lockLeaseSeconds", 600L);
        ReflectionTestUtils.setField(scheduler, "reconciliationIntervalSeconds", 300L);
        when(redis.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    locks.put(invocation.getArgument(0), invocation.getArgument(1));
                    return true;
                });
        lenient().when(valueOperations.get(startsWith("cultivation:sensor:cache:refresh-lock:")))
                .thenAnswer(invocation -> locks.get(invocation.getArgument(0)));
        lenient().when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);
        lenient().when(cacheService.appendWithLock(anyLong(), anyList(), any(Duration.class),
                any(Duration.class), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void usesIndependentLockPerCultivation() {
        CultivationSensor first = sensor(1L);
        CultivationSensor second = sensor(2L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(first, second));
        when(influxService.findValuesByCultivationId(anyLong(), any(Duration.class)))
                .thenReturn(List.of());

        scheduler.warmUp();

        verify(valueOperations).setIfAbsent(
                eq("cultivation:sensor:cache:refresh-lock:1"), anyString(), any(Duration.class));
        verify(valueOperations).setIfAbsent(
                eq("cultivation:sensor:cache:refresh-lock:2"), anyString(), any(Duration.class));
    }

    @Test
    void warmUpRemainsIncompleteWhenOneCultivationRefreshFails() {
        CultivationSensor first = sensor(1L);
        CultivationSensor second = sensor(2L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(first, second));
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());
        when(influxService.findValuesByCultivationId(eq(2L), any(Duration.class)))
                .thenThrow(new RuntimeException("influx unavailable"));

        scheduler.warmUp();
        scheduler.poll();

        verify(influxService, times(2)).findValuesByCultivationId(eq(2L), any(Duration.class));
        verify(influxService, times(2)).findValuesByCultivationId(eq(1L), any(Duration.class));
        verify(cacheService, times(2)).appendWithLock(eq(1L), eq(List.of()), any(Duration.class),
                any(Duration.class), anyString(), anyString());
    }

    @Test
    void warmUpRemainsIncompleteWhenCompactionFails() {
        CultivationSensor first = sensor(1L);
        CultivationSensor second = sensor(2L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(first, second));
        when(influxService.findValuesByCultivationId(anyLong(), any(Duration.class)))
                .thenReturn(List.of());
        doThrow(new RuntimeException("compaction unavailable"))
                .when(cacheService).compactCultivation(eq(1L), any(Duration.class), any(Duration.class), anyString(), anyString());

        scheduler.warmUp();

        verify(influxService).findValuesByCultivationId(eq(2L), any(Duration.class));
        verify(cacheService).compactCultivation(eq(1L), any(Duration.class), any(Duration.class), anyString(), anyString());
        verify(cacheService).compactCultivation(eq(2L), any(Duration.class), any(Duration.class), anyString(), anyString());
        scheduler.poll();
        verify(influxService, times(2)).findValuesByCultivationId(eq(1L), any(Duration.class));
    }

    @Test
    void doesNotAppendAfterLockOwnershipIsLost() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any())).thenReturn(List.of(sensor));
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());
        AtomicInteger ownershipChecks = new AtomicInteger();
        when(valueOperations.get(startsWith("cultivation:sensor:cache:refresh-lock:")))
                .thenAnswer(invocation -> ownershipChecks.getAndIncrement() == 0
                        ? locks.get(invocation.getArgument(0)) : null);

        scheduler.warmUp();

        verifyNoInteractions(cacheService);
        verify(valueOperations, never()).set(eq("cultivation:sensor:cache:watermark:1"),
                anyString(), any(Duration.class));
        verifyNoInteractions(sensorConnectionService);
    }

    @Test
    void pollDoesNotRefreshWhenDistributedLockIsHeld() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        scheduler.poll();

        verifyNoInteractions(influxService, cacheService);
    }

    @Test
    void pollUsesWatermarkAndOverlapAfterWarmUp() {
        CultivationSensor sensor = sensor(1L);
        Instant watermark = Instant.parse("2026-09-02T00:00:00Z");
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(valueOperations.get("cultivation:sensor:cache:watermark:1"))
                .thenReturn(watermark.toString());
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());

        ReflectionTestUtils.setField(scheduler, "warmedUp", true);
        ReflectionTestUtils.setField(scheduler, "lastReconciliationAt", Instant.now());
        scheduler.poll();

        verify(influxService).findValuesByCultivationId(eq(1L), argThat(duration ->
                duration.compareTo(Duration.ofSeconds(60)) >= 0));
    }

    @Test
    void malformedWatermarkFallsBackToConfiguredRange() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(valueOperations.get("cultivation:sensor:cache:watermark:1"))
                .thenReturn("not-an-instant");
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());

        ReflectionTestUtils.setField(scheduler, "warmedUp", true);
        ReflectionTestUtils.setField(scheduler, "lastReconciliationAt", Instant.now());
        scheduler.poll();

        verify(influxService).findValuesByCultivationId(1L, Duration.ofHours(12));
    }

    @Test
    void reusesCultivationSnapshotDuringCacheWindow() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());

        scheduler.warmUp();
        scheduler.poll();

        verify(sensorRepository, times(1)).findAllForDataGeneratorSnapshot(any());
        verify(influxService, times(2)).findValuesByCultivationId(eq(1L), any(Duration.class));
    }

    @Test
    void treatsAppendFailureAsRefreshFailureWithoutWritingWatermark() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());
        when(cacheService.appendWithLock(eq(1L), anyList(), any(Duration.class),
                any(Duration.class), anyString(), anyString())).thenReturn(false);

        scheduler.warmUp();

        verify(cacheService, times(3)).appendWithLock(eq(1L), eq(List.of()), any(Duration.class),
                any(Duration.class), anyString(), anyString());
        verify(valueOperations, never()).set(eq("cultivation:sensor:cache:watermark:1"),
                anyString(), any(Duration.class));
        verifyNoInteractions(sensorConnectionService);
    }

    @Test
    void isolatesRedisLockAcquisitionFailureFromScheduler() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis unavailable"));

        scheduler.poll();

        verifyNoInteractions(influxService, cacheService);
    }

    @Test
    void skipsWatermarkWhenInfluxReturnsNull() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(null);

        scheduler.warmUp();

        verify(cacheService, never()).appendWithLock(anyLong(), anyList(), any(Duration.class),
                any(Duration.class), anyString(), anyString());
        verify(valueOperations, never()).set(eq("cultivation:sensor:cache:watermark:1"),
                anyString(), any(Duration.class));
    }

    @Test
    void usesConfiguredRangeWhenWatermarkIsMissing() {
        CultivationSensor sensor = sensor(1L);
        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(sensor));
        when(valueOperations.get("cultivation:sensor:cache:watermark:1")).thenReturn(null);
        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());
        ReflectionTestUtils.setField(scheduler, "warmedUp", true);
        ReflectionTestUtils.setField(scheduler, "lastReconciliationAt", Instant.now());

        scheduler.poll();

        verify(influxService).findValuesByCultivationId(eq(1L), argThat(duration ->
                duration.equals(Duration.ofHours(12))));
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownHeartbeatExecutor();
    }

    @Test
    void synchronizesConnectionAfterSuccessfulQueryAndCacheAppend() {
        var point = new LatestSensorValueResponse(
                1L, "TEMPERATURE", "°C", BigDecimal.valueOf(23),
                Instant.now().minusSeconds(1),
                "EUI-A", "MODEL-A", "센서 A", "서울", "선반 1");
        var points = List.of(point);
        var queryStartedAt = new AtomicReference<Instant>();

        CultivationSensor registeredSensor = sensor(1L);

        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(registeredSensor));

        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenAnswer(invocation -> {
                    queryStartedAt.set(Instant.now());
                    return points;
                });

        Instant before = Instant.now();
        scheduler.warmUp();

        var order = inOrder(cacheService, sensorConnectionService);
        order.verify(cacheService).appendWithLock(
                eq(1L), same(points), any(Duration.class), any(Duration.class),
                anyString(), anyString());
        order.verify(sensorConnectionService).synchronize(
                eq(1L), same(points),
                argThat(at -> at != null && !at.isBefore(before)
                        && !at.isAfter(queryStartedAt.get())));
    }

    @Test
    void synchronizesConnectionEvenWhenQueryResultIsEmpty() {
        CultivationSensor registeredSensor = sensor(1L);

        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(registeredSensor));

        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenReturn(List.of());

        scheduler.warmUp();

        verify(sensorConnectionService)
                .synchronize(eq(1L), eq(List.of()), any(Instant.class));
    }

    @Test
    void doesNotSynchronizeConnectionWhenInfluxQueryFails() {
        CultivationSensor registeredSensor = sensor(1L);

        when(sensorRepository.findAllForDataGeneratorSnapshot(any()))
                .thenReturn(List.of(registeredSensor));

        when(influxService.findValuesByCultivationId(eq(1L), any(Duration.class)))
                .thenThrow(new IllegalStateException("influx unavailable"));

        scheduler.warmUp();

        verifyNoInteractions(sensorConnectionService, cacheService);
    }

    private CultivationSensor sensor(long cultivationId) {
        CultivationSensor sensor = mock(CultivationSensor.class);
        when(sensor.getCultivationId()).thenReturn(cultivationId);
        return sensor;
    }
}
