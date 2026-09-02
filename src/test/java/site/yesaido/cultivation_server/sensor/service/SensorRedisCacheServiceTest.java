package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorRedisCacheServiceTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisOperations<String, String> redisOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private SensorRedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SensorRedisCacheService(redis, objectMapper);
    }

    @Test
    void emptyPointsDoNotExtendLatestTtl() {
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback<?>) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.exec()).thenReturn(List.of());
        cacheService.append(42L, List.of(), Duration.ofHours(12), Duration.ofSeconds(3));

        verify(redisOperations, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void separatesHistoryAndLatestByUnit() {
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback<?>) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForZSet()).thenReturn(zSetOperations);
        when(redisOperations.opsForHash()).thenReturn(hashOperations);
        when(redisOperations.exec()).thenReturn(List.of());
        when(zSetOperations.rangeByScore(anyString(), any(Double.class), any(Double.class))).thenReturn(Set.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        Instant measuredAt = Instant.parse("2026-09-02T00:00:00Z");
        cacheService.append(42L, List.of(
                point(measuredAt, "C"),
                point(measuredAt, "%")
        ), Duration.ofHours(12), Duration.ofSeconds(3));

        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(zSetOperations, times(2)).add(keys.capture(), eq(measuredAt.toString()), eq((double) measuredAt.toEpochMilli()));
        org.assertj.core.api.Assertions.assertThat(keys.getAllValues()).doesNotHaveDuplicates();
        verify(hashOperations, times(2)).put(anyString(), eq(measuredAt.toString()), eq("{}"));
        var fields = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(hashOperations, times(4)).put(anyString(), fields.capture(), eq("{}"));
        assertThat(fields.getAllValues()).contains("EUI-001|TEMPERATURE|Qw", "EUI-001|TEMPERATURE|JQ");
    }


    private LatestSensorValueResponse point(Instant measuredAt, String unit) {
        return new LatestSensorValueResponse(42L, "TEMPERATURE", unit, BigDecimal.ONE,
                measuredAt, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");
    }
}
