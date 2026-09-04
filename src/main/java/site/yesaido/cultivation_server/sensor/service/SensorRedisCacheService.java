package site.yesaido.cultivation_server.sensor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointResponse;
import site.yesaido.cultivation_server.sensor.support.SensorUnits;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorRedisCacheService {
    private static final String HISTORY_PREFIX = "cultivation:sensor:history:v2:";
    private static final String HISTORY_VALUES_SUFFIX = ":values";
    private static final String COMPACTION_PREFIX = "cultivation:sensor:compaction:v2:";
    private static final String LATEST_PREFIX = "cultivation:sensor:latest:v2:";
    private static final String LATEST_TIMESTAMPS_PREFIX = "cultivation:sensor:latest-timestamps:v2:";
    private static final DefaultRedisScript<Long> RENAME_COMPACTION_SCRIPT =
            redisScript("scripts/redis/rename-compaction.lua");
    private static final DefaultRedisScript<Long> UPDATE_LATEST_SCRIPT =
            redisScript("scripts/redis/update-latest.lua");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void append(long cultivationId, List<LatestSensorValueResponse> points, Duration history, Duration ttlGrace) {
        appendInternal(cultivationId, points, history, ttlGrace, null, null);
    }

    public boolean appendWithLock(long cultivationId, List<LatestSensorValueResponse> points,
                                  Duration history, Duration ttlGrace, String lockKey, String token) {
        return appendInternal(cultivationId, points, history, ttlGrace, lockKey, token);
    }

    private boolean appendInternal(long cultivationId, List<LatestSensorValueResponse> points,
                                   Duration history, Duration ttlGrace, String lockKey, String token) {
        Map<String, Set<String>> expiredMembers = findExpiredMembers(cultivationId, points, history);
        return executeAppendTransaction(cultivationId, points, history, ttlGrace, lockKey, token, expiredMembers);
    }

    private Map<String, Set<String>> findExpiredMembers(long cultivationId,
                                                         List<LatestSensorValueResponse> points,
                                                         Duration history) {
        Map<String, Set<String>> expiredMembers = new HashMap<>();
        double minimumScore = Instant.now().minus(history).toEpochMilli();
        for (LatestSensorValueResponse point : points) {
            if (isAppendable(point)) {
                String historyKey = historyKey(cultivationId, point.deviceEui(), point.sensorType(), point.unit());
                expiredMembers.computeIfAbsent(historyKey, ignored -> expiredMembers(historyKey, minimumScore));
            }
        }
        return expiredMembers;
    }

    private Set<String> expiredMembers(String historyKey, double minimumScore) {
        Set<String> values = redis.opsForZSet().rangeByScore(historyKey, 0, minimumScore);
        return values == null ? Set.of() : values;
    }

    private boolean isAppendable(LatestSensorValueResponse point) {
        return point.deviceEui() != null && point.sensorType() != null
                && point.measuredAt() != null && point.value() != null;
    }

    private boolean executeAppendTransaction(long cultivationId, List<LatestSensorValueResponse> points,
                                             Duration history, Duration ttlGrace, String lockKey, String token,
                                             Map<String, Set<String>> expiredMembers) {
        return Boolean.TRUE.equals(redis.execute(new SessionCallback<Boolean>() {
            @Override
            public Boolean execute(RedisOperations operations) {
                if (lockKey != null) {
                    operations.watch(lockKey);
                    if (!token.equals(operations.opsForValue().get(lockKey))) {
                        operations.unwatch();
                        return false;
                    }
                }
                operations.multi();
                try {
                    appendWithinTransaction(operations, cultivationId, points, history, ttlGrace, expiredMembers);
                    if (lockKey != null) {
                        updateLatestValues(operations, cultivationId, points, history.plus(ttlGrace));
                    }
                    List<Object> result = operations.exec();
                    if (transactionAborted(result)) {
                        return false;
                    }
                    if (lockKey == null) {
                        updateLatestValues(redis, cultivationId, points, history.plus(ttlGrace));
                    }
                    compactHistories(expiredMembers.keySet(), history, ttlGrace, lockKey, token);
                    return true;
                } catch (RuntimeException e) {
                    operations.discard();
                    throw e;
                }
            }
        }));
    }

    private boolean transactionAborted(List<Object> result) {
        return result == null;
    }

    private void appendWithinTransaction(RedisOperations<String, String> operations,
                                         long cultivationId,
                                         List<LatestSensorValueResponse> points,
                                         Duration history,
                                         Duration ttlGrace,
                                         Map<String, Set<String>> expiredMembers) {
        Instant now = Instant.now();
        double minimumScore = now.minus(history).toEpochMilli();
        for (LatestSensorValueResponse point : points) {
            if (point.deviceEui() != null && point.sensorType() != null && point.measuredAt() != null
                    && point.value() != null) {
                String serialized = serialize(point);
                String historyKey = historyKey(cultivationId, point.deviceEui(), point.sensorType(), point.unit());
                String measuredAt = point.measuredAt().toString();
                String valuesKey = historyKey + HISTORY_VALUES_SUFFIX;
                operations.opsForZSet().add(historyKey, measuredAt, point.measuredAt().toEpochMilli());
                operations.opsForHash().put(valuesKey, measuredAt, serialized);
                Set<String> expired = expiredMembers.getOrDefault(historyKey, Set.of());
                if (expired != null && !expired.isEmpty()) {
                    operations.opsForHash().delete(valuesKey, expired.toArray());
                }
                operations.opsForZSet().removeRangeByScore(historyKey, 0, minimumScore);
                operations.expire(historyKey, history.plus(ttlGrace));
                operations.expire(valuesKey, history.plus(ttlGrace));
            }
        }
    }

    private void updateLatestValues(RedisOperations<String, String> operations,
                                    long cultivationId, List<LatestSensorValueResponse> points, Duration ttl) {
        String latestKey = LATEST_PREFIX + cultivationId;
        String timestampKey = LATEST_TIMESTAMPS_PREFIX + cultivationId;
        long ttlSeconds = Math.max(1, ttl.toSeconds());
        for (LatestSensorValueResponse point : points) {
            if (point.deviceEui() != null && point.sensorType() != null && point.measuredAt() != null) {
                String serialized = serialize(point);
                operations.execute(
                        UPDATE_LATEST_SCRIPT,
                        List.of(latestKey, timestampKey),
                        latestField(point), serialized, String.valueOf(point.measuredAt().toEpochMilli()), String.valueOf(ttlSeconds)
                );
            }
        }
    }

    private void compactHistories(Set<String> historyKeys, Duration history, Duration ttlGrace,
                                  String lockKey, String token) {
        Instant now = Instant.now();
        for (String historyKey : historyKeys) {
            compactHistory(historyKey, history, ttlGrace, now, lockKey, token);
        }
    }

    public int compactCultivation(long cultivationId, Duration history, Duration ttlGrace) {
        return compactCultivation(cultivationId, history, ttlGrace, null, null);
    }

    public int compactCultivation(long cultivationId, Duration history, Duration ttlGrace,
                                  String lockKey, String token) {
        String prefix = HISTORY_PREFIX + cultivationId + ":";
        int compacted = 0;
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                String historyKey = cursor.next();
                if (!historyKey.startsWith(prefix) || historyKey.endsWith(HISTORY_VALUES_SUFFIX)) {
                    continue;
                }
                compactHistory(historyKey, history, ttlGrace, Instant.now(), lockKey, token);
                compacted++;
            }
        }
        return compacted;
    }

    private void compactHistory(String historyKey, Duration history, Duration ttlGrace, Instant now,
                                 String lockKey, String token) {
        Set<String> members = redis.opsForZSet().range(historyKey, 0, -1);
        if (members == null || members.isEmpty()) {
            redis.delete(historyKey + HISTORY_VALUES_SUFFIX);
            return;
        }

        List<LatestSensorValueResponse> points = redis.opsForHash()
                .multiGet(historyKey + HISTORY_VALUES_SUFFIX, List.copyOf(members)).stream()
                .filter(String.class::isInstance)
                .map(value -> deserialize((String) value))
                .filter(Objects::nonNull)
                .filter(point -> point.measuredAt() != null
                        && !point.measuredAt().isBefore(now.minus(history)))
                .toList();
        Map<String, List<LatestSensorValueResponse>> buckets = bucketPoints(points, now);
        if (buckets.isEmpty()) {
            deleteHistory(historyKey);
            return;
        }

        String tempHistoryKey = COMPACTION_PREFIX + UUID.randomUUID();
        String tempValuesKey = tempHistoryKey + HISTORY_VALUES_SUFFIX;
        try {
            boolean wroteBucket = writeBuckets(buckets, tempHistoryKey, tempValuesKey, now);
            if (!wroteBucket) {
                deleteHistory(historyKey);
                return;
            }

            redis.expire(tempHistoryKey, history.plus(ttlGrace));
            redis.expire(tempValuesKey, history.plus(ttlGrace));
            Long renamed = lockKey == null
                    ? redis.execute(RENAME_COMPACTION_SCRIPT,
                    List.of(historyKey, historyKey + HISTORY_VALUES_SUFFIX, tempHistoryKey, tempValuesKey))
                    : redis.execute(RENAME_COMPACTION_SCRIPT,
                    List.of(historyKey, historyKey + HISTORY_VALUES_SUFFIX, tempHistoryKey, tempValuesKey, lockKey), token);
            if (lockKey != null && !Long.valueOf(1L).equals(renamed)) {
                throw new IllegalStateException("compaction lock ownership lost");
            }
        } finally {
            redis.delete(List.of(tempHistoryKey, tempValuesKey));
        }
    }

    private Map<String, List<LatestSensorValueResponse>> bucketPoints(
            List<LatestSensorValueResponse> points, Instant now) {
        Map<String, List<LatestSensorValueResponse>> buckets = new HashMap<>();
        for (LatestSensorValueResponse point : points) {
            long ageSeconds = Math.max(0, Duration.between(point.measuredAt(), now).getSeconds());
            long resolution = resolutionForAge(ageSeconds);
            long bucket = bucketStart(point.measuredAt(), resolution);
            buckets.computeIfAbsent(resolution + ":" + bucket, ignored -> new ArrayList<>()).add(point);
        }
        return buckets;
    }

    private boolean writeBuckets(
            Map<String, List<LatestSensorValueResponse>> buckets,
            String tempHistoryKey,
            String tempValuesKey,
            Instant now) {
        boolean wroteBucket = false;
        for (List<LatestSensorValueResponse> bucketPoints : buckets.values()) {
            LatestSensorValueResponse averaged = averageBucket(bucketPoints, now);
            if (averaged != null) {
                String member = averaged.measuredAt().toString();
                redis.opsForZSet().add(tempHistoryKey, member, averaged.measuredAt().toEpochMilli());
                redis.opsForHash().put(tempValuesKey, member, serialize(averaged));
                wroteBucket = true;
            }
        }
        return wroteBucket;
    }

    private LatestSensorValueResponse averageBucket(
            List<LatestSensorValueResponse> bucketPoints, Instant now) {
        LatestSensorValueResponse first = bucketPoints.getFirst();
        long ageSeconds = Math.max(0, Duration.between(first.measuredAt(), now).getSeconds());
        long resolution = resolutionForAge(ageSeconds);
        long bucket = bucketStart(first.measuredAt(), resolution);
        List<BigDecimal> values = bucketPoints.stream()
                .map(LatestSensorValueResponse::value)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) return null;

        BigDecimal average = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, java.math.RoundingMode.HALF_UP);
        Instant measuredAt = Instant.ofEpochSecond(bucket);
        return new LatestSensorValueResponse(
                first.cultivationId(), first.sensorType(), first.unit(), average, measuredAt,
                first.deviceEui(), first.deviceModel(), first.deviceName(), first.location(), first.place());
    }

    private long resolutionForAge(long ageSeconds) {
        if (ageSeconds <= 60) return 3;
        if (ageSeconds <= 300) return 10;
        if (ageSeconds <= 3600) return 60;
        return 300;
    }

    private long bucketStart(Instant measuredAt, long resolution) {
        return (measuredAt.getEpochSecond() / resolution) * resolution;
    }

    private void deleteHistory(String historyKey) {
        redis.delete(List.of(historyKey, historyKey + HISTORY_VALUES_SUFFIX));
    }

    public List<LatestSensorValueResponse> findHistory(long cultivationId, Duration history) {
        return findHistory(Set.of(cultivationId), history);
    }

    public Map<Long, List<LatestSensorValueResponse>> findHistory(List<Long> cultivationIds, Duration history) {
        Set<Long> ids = cultivationIds == null ? Set.of() : cultivationIds.stream()
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<Long, List<LatestSensorValueResponse>> result = new HashMap<>();
        for (Long id : ids) result.put(id, new ArrayList<>());
        Instant threshold = Instant.now().minus(history);
        ScanOptions options = ScanOptions.scanOptions().match(HISTORY_PREFIX + "*").count(100).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                addHistoryForKey(cursor.next(), ids, threshold, result);
            }
        }
        result.replaceAll((id, points) -> points.stream()
                .sorted(Comparator.comparing(LatestSensorValueResponse::measuredAt)).toList());
        return result;
    }

    private void addHistoryForKey(
            String key,
            Set<Long> ids,
            Instant threshold,
            Map<Long, List<LatestSensorValueResponse>> result) {
        Long cultivationId = cultivationIdFromHistoryKey(key);
        if (cultivationId == null || !ids.contains(cultivationId)) return;
        Set<String> members = redis.opsForZSet().rangeByScore(key, threshold.toEpochMilli(), Double.MAX_VALUE);
        if (members == null || members.isEmpty()) return;
        redis.opsForHash().multiGet(key + HISTORY_VALUES_SUFFIX, List.copyOf(members)).stream()
                .filter(String.class::isInstance).map(value -> deserialize((String) value))
                .filter(Objects::nonNull)
                .filter(point -> point.measuredAt() != null && !point.measuredAt().isBefore(threshold))
                .forEach(point -> result.get(cultivationId).add(point));
    }

    private Long cultivationIdFromHistoryKey(String key) {
        if (!key.startsWith(HISTORY_PREFIX) || key.endsWith(HISTORY_VALUES_SUFFIX)) return null;
        String remainder = key.substring(HISTORY_PREFIX.length());
        int separator = remainder.indexOf(':');
        if (separator < 1) return null;
        try {
            return Long.valueOf(remainder.substring(0, separator));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<LatestSensorValueResponse> findHistory(Set<Long> cultivationIds, Duration history) {
        return findHistory(new ArrayList<>(cultivationIds), history).values().stream()
                .flatMap(List::stream).toList();
    }

    public SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, String sensorType, String unit) {
        String key = historyKey(cultivationId, deviceEui, sensorType, unit);
        Set<String> members = redis.opsForZSet().range(key, 0, -1);
        List<LatestSensorValueResponse> points = members == null || members.isEmpty()
                ? List.of()
                : redis.opsForHash().multiGet(key + HISTORY_VALUES_SUFFIX, List.copyOf(members)).stream()
                .filter(String.class::isInstance)
                .map(value -> deserialize((String) value))
                .filter(Objects::nonNull)
                .toList();
        if (points.isEmpty()) {
            return null;
        }

        List<SensorTrendPointResponse> trend = points.stream()
                .filter(point -> point.measuredAt() != null && point.value() != null)
                .sorted(Comparator.comparing(LatestSensorValueResponse::measuredAt))
                .map(point -> new SensorTrendPointResponse(point.measuredAt(), point.value()))
                .toList();
        LatestSensorValueResponse first = points.getFirst();
        return new SensorTrendPointListResponse(cultivationId, deviceEui, sensorType, first.unit(), trend);
    }

    public record LatestCacheReadResult(
            List<LatestSensorValueResponse> points,
            boolean hasStaleValues
    ) {
    }

    public LatestCacheReadResult findLatestWithStatus(long cultivationId, Duration freshness) {
        List<LatestSensorValueResponse> all = findLatest(cultivationId);
        Instant threshold = Instant.now().minus(freshness);
        List<LatestSensorValueResponse> fresh = all.stream()
                .filter(point -> point.measuredAt() != null && !point.measuredAt().isBefore(threshold))
                .toList();
        return new LatestCacheReadResult(fresh, all.size() > fresh.size());
    }

    public List<LatestSensorValueResponse> findLatest(long cultivationId, Duration freshness) {
        Instant threshold = Instant.now().minus(freshness);
        return findLatest(cultivationId).stream()
                .filter(point -> point.measuredAt() != null && !point.measuredAt().isBefore(threshold))
                .toList();
    }

    public Map<Long, List<LatestSensorValueResponse>> findLatest(List<Long> cultivationIds, Duration freshness) {
        List<Long> ids = cultivationIds == null ? List.of() : cultivationIds.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Instant threshold = Instant.now().minus(freshness);
        List<Object> responses = redis.executePipelined((RedisCallback<Object>) connection -> {
            for (Long id : ids) {
                connection.hashCommands().hGetAll((LATEST_PREFIX + id).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        Map<Long, List<LatestSensorValueResponse>> result = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            Object raw = responses.get(i);
            if (raw instanceof Map<?, ?> values) {
                List<LatestSensorValueResponse> points = values.values().stream()
                        .map(this::pipelineValue)
                        .filter(java.util.Objects::nonNull)
                        .filter(point -> point.measuredAt() != null && !point.measuredAt().isBefore(threshold))
                        .sorted(Comparator.comparing(LatestSensorValueResponse::sensorType,
                                Comparator.nullsLast(String::compareTo)))
                        .toList();
                if (!points.isEmpty()) result.put(ids.get(i), points);
            }
        }
        return result;
    }

    private LatestSensorValueResponse pipelineValue(Object value) {
        if (value instanceof byte[] bytes) return deserialize(new String(bytes, StandardCharsets.UTF_8));
        if (value instanceof String text) return deserialize(text);
        return null;
    }

    public List<LatestSensorValueResponse> findLatest(long cultivationId) {
        Map<Object, Object> values = redis.opsForHash().entries(LATEST_PREFIX + cultivationId);
        return values.values().stream()
                .filter(String.class::isInstance)
                .map(value -> deserialize((String) value))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(LatestSensorValueResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }



    private String historyKey(long cultivationId, String deviceEui, String sensorType, String unit) {
        return HISTORY_PREFIX + cultivationId + ":" + encodeSegment(deviceEui) + ":" + encodeSegment(sensorType) + ":" + encodeUnit(unit);
    }

    private String latestField(LatestSensorValueResponse point) {
        return encodeSegment(point.deviceEui()) + "|" + encodeSegment(point.sensorType()) + "|" + encodeUnit(point.unit());
    }

    private String encodeSegment(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeUnit(String unit) {
        String normalized = SensorUnits.normalize(unit);
        if (normalized == null) {
            normalized = "<none>";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private static DefaultRedisScript<Long> redisScript(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(Long.class);
        return script;
    }

    private String serialize(LatestSensorValueResponse point) {
        try {
            return objectMapper.writeValueAsString(point);
        } catch (Exception e) {
            throw new IllegalStateException("센서 캐시 직렬화에 실패했습니다.", e);
        }
    }

    private LatestSensorValueResponse deserialize(String value) {
        try {
            return objectMapper.readValue(value, LatestSensorValueResponse.class);
        } catch (Exception e) {
            log.warn("센서 캐시 데이터 파싱 실패: 데이터는 기록하지 않고 건너뜁니다.");
            return null;
        }
    }
}
