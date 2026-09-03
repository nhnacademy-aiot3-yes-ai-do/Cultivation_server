package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorCacheSchedulerTest {
    @Mock
    private CultivationSensorRepository sensorRepository;
    @Mock
    private InfluxService influxService;
    @Mock
    private SensorRedisCacheService cacheService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private SensorCacheScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SensorCacheScheduler(sensorRepository, influxService, cacheService, redis);
        ReflectionTestUtils.setField(scheduler, "historyHours", 12L);
        ReflectionTestUtils.setField(scheduler, "ttlGraceSeconds", 3L);
        ReflectionTestUtils.setField(scheduler, "queryOverlapSeconds", 60L);
        ReflectionTestUtils.setField(scheduler, "lockLeaseSeconds", 600L);
        ReflectionTestUtils.setField(scheduler, "reconciliationIntervalSeconds", 300L);
        when(redis.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);
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
        verify(cacheService, times(2)).append(eq(1L), eq(List.of()), any(Duration.class), any(Duration.class));
    }

    @Test
    void pollDoesNotRefreshWhenDistributedLockIsHeld() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        scheduler.poll();

        verifyNoInteractions(sensorRepository, influxService, cacheService);
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

        verify(influxService).findValuesByCultivationId(eq(1L), eq(Duration.ofHours(12)));
    }

    private CultivationSensor sensor(long cultivationId) {
        CultivationSensor sensor = mock(CultivationSensor.class);
        when(sensor.getCultivationId()).thenReturn(cultivationId);
        return sensor;
    }
}
