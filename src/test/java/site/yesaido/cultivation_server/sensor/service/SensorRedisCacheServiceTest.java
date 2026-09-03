package site.yesaido.cultivation_server.sensor.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
        when(redisOperations.exec()).thenReturn(List.of());
        cacheService.append(42L, List.of(), Duration.ofHours(12), Duration.ofSeconds(3));

        verify(redisOperations, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void separatesHistoryAndLatestByUnit() {
        when(redis.opsForZSet()).thenReturn(zSetOperations);

        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
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
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
        verify(hashOperations, times(2)).put(anyString(), eq(measuredAt.toString()), eq("{}"));
        verify(redis, times(2)).execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
                anyList(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void skipsIncompletePointsButStillCompletesTransaction() {
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
        when(redisOperations.exec()).thenReturn(List.of());

        cacheService.append(42L, List.of(
                new LatestSensorValueResponse(42L, null, "C", BigDecimal.ONE,
                        Instant.now(), "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE"),
                new LatestSensorValueResponse(42L, "TEMPERATURE", "C", BigDecimal.ONE,
                        null, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE")
        ), Duration.ofHours(12), Duration.ofSeconds(3));

        verify(redisOperations).multi();
        verify(redisOperations).exec();
        verify(redis, never()).execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
                anyList(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void discardsTransactionWhenSerializationFails() throws Exception {
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScore(anyString(), any(Double.class), any(Double.class))).thenReturn(Set.of());
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialization failure"));

        LatestSensorValueResponse failingPoint = point(Instant.now(), "C");
        Assertions.assertThatThrownBy(() -> appendFailingPoint(failingPoint))
                .isInstanceOf(IllegalStateException.class);

        verify(redisOperations).discard();
        verify(redisOperations, never()).exec();
    }

    @Test
    void findTrendReturnsNullForMissingOrInvalidValues() {
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Set.of("missing"));
        when(hashOperations.multiGet(anyString(), anyList())).thenReturn(List.of("invalid-json"));
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("invalid"));

        var response = cacheService.findTrend(42L, "EUI-001", "TEMPERATURE", "C");

        assertThat(response).isNull();
    }

    @Test
    void findTrendReturnsStoredResolutionBuckets() throws Exception {
        Instant first = Instant.parse("2026-09-02T00:01:00Z");
        Instant second = Instant.parse("2026-09-02T00:14:00Z");
        LatestSensorValueResponse firstPoint = point(first, "C");
        LatestSensorValueResponse secondPoint = new LatestSensorValueResponse(42L, "TEMPERATURE", "C",
                BigDecimal.valueOf(3), second, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Set.of(first.toString(), second.toString()));
        when(hashOperations.multiGet(anyString(), anyList())).thenReturn(List.of("first", "second"));
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(firstPoint, secondPoint);

        var response = cacheService.findTrend(42L, "EUI-001", "TEMPERATURE", "C");

        assertThat(response.responses()).hasSize(2);
        assertThat(response.responses().get(0).value()).isEqualByComparingTo("1");
        assertThat(response.responses().get(1).value()).isEqualByComparingTo("3");
    }

    @Test
    void findLatestFiltersStaleAndNullMeasuredAtPoints() throws Exception {
        Instant fresh = Instant.now();
        LatestSensorValueResponse freshPoint = point(fresh, "C");
        LatestSensorValueResponse stalePoint = point(fresh.minusSeconds(10), "C");
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of("fresh", "fresh", "stale", "stale", "bad", 1));
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(freshPoint, stalePoint);

        var response = cacheService.findLatest(42L, Duration.ofSeconds(3));

        assertThat(response).containsExactly(freshPoint);
    }

    @Test
    void findLatestBatchHandlesNullIdsAndPipelineValueTypes() throws Exception {
        Instant fresh = Instant.now();
        LatestSensorValueResponse point = point(fresh, "C");
        when(redis.executePipelined(any(RedisCallback.class))).thenReturn(List.of(
                Map.of("a", "text"),
                Map.of("b", "bytes".getBytes(StandardCharsets.UTF_8)),
                Map.of("c", 1)));
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(point, point);

        var response = cacheService.findLatest(java.util.Arrays.asList(42L, null, 42L, 43L, 44L), Duration.ofSeconds(3));

        assertThat(response)
                .containsKeys(42L, 43L)
                .doesNotContainKey(44L);
        assertThat(response.get(42L)).containsExactly(point);
    }

    @Test
    void findLatestBatchReturnsEmptyForNullOrEmptyIds() {
        assertThat(cacheService.findLatest(null, Duration.ofSeconds(3))).isEmpty();
        assertThat(cacheService.findLatest(List.of(), Duration.ofSeconds(3))).isEmpty();
        verify(redis, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void findLatestSkipsInvalidSerializedValues() throws Exception {
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of("bad", "invalid"));
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("invalid"));

        assertThat(cacheService.findLatest(42L)).isEmpty();
    }


    private LatestSensorValueResponse point(Instant measuredAt, String unit) {
        return new LatestSensorValueResponse(42L, "TEMPERATURE", unit, BigDecimal.ONE,
                measuredAt, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");
    }

    private void appendFailingPoint(LatestSensorValueResponse failingPoint) {
        cacheService.append(42L, List.of(failingPoint), Duration.ofHours(12), Duration.ofSeconds(3));
    }
}
