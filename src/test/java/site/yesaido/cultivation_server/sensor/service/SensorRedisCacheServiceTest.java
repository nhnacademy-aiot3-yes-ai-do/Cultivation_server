package site.yesaido.cultivation_server.sensor.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    void appendWithLockReturnsFalseWhenTokenDoesNotMatch() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            when(redisOperations.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("lock")).thenReturn("another-token");
            return callback.execute(redisOperations);
        });

        boolean appended = cacheService.appendWithLock(42L, List.of(), Duration.ofHours(12),
                Duration.ofSeconds(3), "lock", "expected-token");

        assertThat(appended).isFalse();
        verify(redisOperations).watch("lock");
        verify(redisOperations).unwatch();
        verify(redisOperations, never()).multi();
    }

    @Test
    void appendWithLockReturnsFalseWhenTransactionIsAborted() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            when(redisOperations.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("lock")).thenReturn("expected-token");
            when(redisOperations.exec()).thenReturn(null);
            return callback.execute(redisOperations);
        });

        boolean appended = cacheService.appendWithLock(42L, List.of(), Duration.ofHours(12),
                Duration.ofSeconds(3), "lock", "expected-token");

        assertThat(appended).isFalse();
        verify(redisOperations).watch("lock");
        verify(redisOperations).multi();
        verify(redisOperations).exec();
        verify(redis, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }

    @Test
    void compactCultivationDeletesOrphanValuesKeyForEmptyHistory() {
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("cultivation:sensor:history:v2:42:eui:type:unit");
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Set.of());

        int compacted = cacheService.compactCultivation(42L, Duration.ofHours(12), Duration.ofSeconds(3));

        assertThat(compacted).isEqualTo(1);
        verify(redis).delete("cultivation:sensor:history:v2:42:eui:type:unit:values");
    }

    @Test
    void compactCultivationSkipsValuesKeysAndOtherCultivations() {
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(
                "cultivation:sensor:history:v2:42:eui:type:unit:values",
                "cultivation:sensor:history:v2:999:eui:type:unit");
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

        int compacted = cacheService.compactCultivation(42L, Duration.ofHours(12), Duration.ofSeconds(3));

        assertThat(compacted).isZero();
        verify(redis, never()).opsForZSet();
    }

    @Test
    void findHistoryReturnsEmptyForNullAndEmptyCultivationIds() {
        assertThat(cacheService.findHistory((List<Long>) null, Duration.ofHours(1))).isEmpty();
        assertThat(cacheService.findHistory(List.of(), Duration.ofHours(1))).isEmpty();
        verify(redis, never()).scan(any(ScanOptions.class));
    }

    @Test
    void findTrendReturnsEmptyTrendWhenStoredPointsHaveNoMeasuredAtOrValue() throws Exception {
        Instant now = Instant.now();
        LatestSensorValueResponse invalid = new LatestSensorValueResponse(
                42L, "TEMPERATURE", "C", null, null, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Set.of(now.toString()));
        when(hashOperations.multiGet(anyString(), anyList())).thenReturn(List.of("invalid"));
        when(objectMapper.readValue("invalid", LatestSensorValueResponse.class)).thenReturn(invalid);

        var response = cacheService.findTrend(42L, "EUI-001", "TEMPERATURE", "C");

        assertThat(response).isNotNull();
        assertThat(response.responses()).isEmpty();
    }

    @Test
    void findTrendReturnsNullWhenHistoryMembersAreMissing() {
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

        assertThat(cacheService.findTrend(42L, "EUI-001", "TEMPERATURE", "C")).isNull();
    }

    @Test
    void distinguishesStaleValuesFromEmptyLatestCache() {
        when(redis.opsForHash()).thenReturn(hashOperations);
        LatestSensorValueResponse stale = point(Instant.now().minusSeconds(10), "C");
        when(hashOperations.entries(anyString())).thenReturn(Map.of("sensor", "stale"));
        when(objectMapper.readValue(eq("stale"), any(Class.class))).thenReturn(stale);

        var result = cacheService.findLatestWithStatus(42L, Duration.ofSeconds(3));

        assertThat(result.points()).isEmpty();
        assertThat(result.hasStaleValues()).isTrue();
    }

    @Test
    void reportsEmptyLatestCacheWhenRedisHasNoValues() {
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of());

        var result = cacheService.findLatestWithStatus(42L, Duration.ofSeconds(3));

        assertThat(result.points()).isEmpty();
        assertThat(result.hasStaleValues()).isFalse();
    }

    @Test
    void findLatestReturnsEmptyWhenStoredEntriesAreNotStrings() {
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of("bad", 123));

        assertThat(cacheService.findLatest(42L)).isEmpty();
    }

    @Test
    void findHistorySkipsMalformedAndValuesKeys() {
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(
                "cultivation:sensor:history:v2:not-a-number:eui:type:unit",
                "cultivation:sensor:history:v2:42:eui:type:unit:values");
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

        var result = cacheService.findHistory(List.of(42L), Duration.ofHours(1));

        assertThat(result).containsKey(42L);
        assertThat(result.get(42L)).isEmpty();
        verify(redis, never()).opsForZSet();
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
        assertThat(cacheService.findLatest(List.<Long>of(), Duration.ofSeconds(3))).isEmpty();
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


    @Test
    void compactsStoredPointsAndUsesBothClasspathScripts() {
        Instant now = Instant.now();
        List<LatestSensorValueResponse> points = List.of(
                point(now.minusSeconds(10), "C"),
                point(now.minusSeconds(120), "C"),
                point(now.minusSeconds(600), "C"),
                point(now.minusSeconds(7_200), "C"));
        Set<String> members = Set.of("p1", "p2", "p3", "p4");
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Set.of());
        when(zSetOperations.range(anyString(), anyLong(), anyLong())).thenReturn(members);
        when(hashOperations.multiGet(anyString(), anyList())).thenReturn(List.of("p1", "p2", "p3", "p4"));
        when(objectMapper.writeValueAsString(any())).thenReturn("serialized");
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenReturn(points.get(0), points.get(1), points.get(2), points.get(3));
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            when(redisOperations.opsForZSet()).thenReturn(zSetOperations);
            when(redisOperations.opsForHash()).thenReturn(hashOperations);
            return callback.execute(redisOperations);
        });
        when(redisOperations.exec()).thenReturn(List.of());

        cacheService.append(42L, points, Duration.ofHours(12), Duration.ofSeconds(3));

        @SuppressWarnings("unchecked")
        var latestScripts = org.mockito.ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis, times(4)).execute(latestScripts.capture(), anyList(),
                anyString(), anyString(), anyString(), anyString());
        assertThat(latestScripts.getValue().getScriptAsString()).contains("HGET", "HSET");

        @SuppressWarnings("unchecked")
        var compactionScripts = org.mockito.ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis).execute(compactionScripts.capture(), anyList());
        assertThat(compactionScripts.getValue().getScriptAsString()).contains("RENAME", "KEYS[4]");
        verify(zSetOperations, atLeastOnce()).add(anyString(), anyString(), anyDouble());
        verify(hashOperations, atLeastOnce()).put(anyString(), anyString(), eq("serialized"));
    }

    @Test
    void loadsRedisLuaScriptsFromClasspathResources() {
        DefaultRedisScript<Long> updateLatest = redisScript("scripts/redis/update-latest.lua");
        DefaultRedisScript<Long> renameCompaction = redisScript("scripts/redis/rename-compaction.lua");

        assertThat(updateLatest.getResultType()).isEqualTo(Long.class);
        assertThat(updateLatest.getScriptAsString()).contains("HGET", "ARGV[4]", "should_update");
        assertThat(renameCompaction.getResultType()).isEqualTo(Long.class);
        assertThat(renameCompaction.getScriptAsString()).contains("RENAME", "KEYS[4]", "KEYS[1]");
        assertThat(new ClassPathResource("scripts/redis/update-latest.lua").exists()).isTrue();
        assertThat(new ClassPathResource("scripts/redis/rename-compaction.lua").exists()).isTrue();
    }

    @Test
    void findHistoryBatchReadsMatchingHistoryAndSkipsValuesKey() throws Exception {
        Instant measuredAt = Instant.now();
        LatestSensorValueResponse point = point(measuredAt, "C");
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, true, true, false);
        when(cursor.next()).thenReturn(
                "cultivation:sensor:history:v2:42:eui:type:unit",
                "cultivation:sensor:history:v2:42:eui:type:unit:values",
                "cultivation:sensor:history:v2:999:eui:type:unit",
                "unrelated-key");
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(Set.of(measuredAt.toString()));
        when(hashOperations.multiGet(anyString(), anyList())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.startsWith("cultivation:sensor:history:v2:42:") ? List.of("point") : List.of();
        });
        when(objectMapper.readValue("point", LatestSensorValueResponse.class)).thenReturn(point);

        var result = cacheService.findHistory(List.of(42L, 999L), Duration.ofHours(1));

        assertThat(result.get(42L)).containsExactly(point);
        assertThat(result.get(999L)).isEmpty();
        verify(zSetOperations).rangeByScore(
                eq("cultivation:sensor:history:v2:42:eui:type:unit"), anyDouble(), anyDouble());
        verify(zSetOperations, never()).rangeByScore(
                eq("cultivation:sensor:history:v2:42:eui:type:unit:values"), anyDouble(), anyDouble());
    }

    @Test
    void nullValueIsIgnoredByAppend() {
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<Object> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
        when(redisOperations.exec()).thenReturn(List.of());
        LatestSensorValueResponse nullValue = new LatestSensorValueResponse(
                42L, "TEMPERATURE", "C", null, Instant.now(),
                "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");

        cacheService.append(42L, List.of(nullValue), Duration.ofHours(12), Duration.ofSeconds(3));

        verify(redisOperations).multi();
        verify(redisOperations).exec();
        verify(redis, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }

    private DefaultRedisScript<Long> redisScript(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }

    private LatestSensorValueResponse point(Instant measuredAt, String unit) {
        return new LatestSensorValueResponse(42L, "TEMPERATURE", unit, BigDecimal.ONE,
                measuredAt, "EUI-001", "MODEL", "NAME", "LOCATION", "PLACE");
    }

    private void appendFailingPoint(LatestSensorValueResponse failingPoint) {
        cacheService.append(42L, List.of(failingPoint), Duration.ofHours(12), Duration.ofSeconds(3));
    }
}
