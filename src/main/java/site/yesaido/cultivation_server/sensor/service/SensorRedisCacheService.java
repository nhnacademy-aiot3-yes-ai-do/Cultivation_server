package site.yesaido.cultivation_server.sensor.service;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointResponse;
import site.yesaido.cultivation_server.sensor.support.SensorUnits;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorRedisCacheService {
    private static final String HISTORY_PREFIX = "cultivation:sensor:history:v2:";
    private static final String HISTORY_VALUES_SUFFIX = ":values";
    private static final String LATEST_PREFIX = "cultivation:sensor:latest:v2:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void append(long cultivationId, List<LatestSensorValueResponse> points, Duration history, Duration ttlGrace) {
        String latestKey = LATEST_PREFIX + cultivationId;
        Map<String, String> latestValues = new HashMap<>();
        Map<String, Set<String>> expiredMembers = new HashMap<>();
        double minimumScore = Instant.now().minus(history).toEpochMilli();
        for (LatestSensorValueResponse point : points) {
            if (point.deviceEui() == null || point.sensorType() == null || point.measuredAt() == null) {
                continue;
            }
            String historyKey = historyKey(cultivationId, point.deviceEui(), point.sensorType(), point.unit());
            String field = latestField(point);
            latestValues.computeIfAbsent(field, ignored -> {
                Object value = redis.opsForHash().get(latestKey, field);
                return value instanceof String existing ? existing : null;
            });
            expiredMembers.computeIfAbsent(historyKey, ignored -> {
                Set<String> values = redis.opsForZSet().rangeByScore(historyKey, 0, minimumScore);
                return values == null ? Set.of() : values;
            });
        }
        redis.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                try {
                    appendWithinTransaction(operations, cultivationId, points, history, ttlGrace,
                            latestValues, expiredMembers);
                    return operations.exec();
                } catch (RuntimeException e) {
                    operations.discard();
                    throw e;
                }
            }
        });
    }

    private void appendWithinTransaction(RedisOperations<String, String> operations,
                                         long cultivationId,
                                         List<LatestSensorValueResponse> points,
                                         Duration history,
                                         Duration ttlGrace,
                                         Map<String, String> latestValues,
                                         Map<String, Set<String>> expiredMembers) {
        Instant now = Instant.now();
        double minimumScore = now.minus(history).toEpochMilli();
        String latestKey = LATEST_PREFIX + cultivationId;

        for (LatestSensorValueResponse point : points) {
            if (point.deviceEui() == null || point.sensorType() == null || point.measuredAt() == null) {
                continue;
            }
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

            String field = latestField(point);
            String existing = latestValues.get(field);
            if (existing == null || isNewer(point, existing)) {
                operations.opsForHash().put(latestKey, field, serialized);
                latestValues.put(field, serialized);
            }
        }
        if (!points.isEmpty()) {
            operations.expire(latestKey, history.plus(ttlGrace));
        }
    }

    public SensorTrendPointListResponse findTrend(long cultivationId, String deviceEui, String sensorType, String unit) {
        String key = historyKey(cultivationId, deviceEui, sensorType, unit);
        Set<String> members = redis.opsForZSet().range(key, 0, -1);
        List<LatestSensorValueResponse> points = members == null || members.isEmpty()
                ? List.of()
                : redis.opsForHash().multiGet(key + HISTORY_VALUES_SUFFIX, List.copyOf(members)).stream()
                .filter(String.class::isInstance)
                .map(value -> deserialize((String) value))
                .filter(point -> point != null)
                .toList();
        if (points.isEmpty()) {
            return null;
        }

        Map<Long, List<BigDecimal>> buckets = new TreeMap<>();
        for (LatestSensorValueResponse point : points) {
            long bucket = (point.measuredAt().getEpochSecond() / 900) * 900;
            buckets.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(point.value());
        }
        List<SensorTrendPointResponse> trend = buckets.entrySet().stream()
                .map(entry -> new SensorTrendPointResponse(
                        Instant.ofEpochSecond(entry.getKey()),
                        entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(entry.getValue().size()), 4, java.math.RoundingMode.HALF_UP)))
                .toList();
        LatestSensorValueResponse first = points.get(0);
        return new SensorTrendPointListResponse(cultivationId, deviceEui, sensorType, first.unit(), trend);
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
            if (!(raw instanceof Map<?, ?> values)) continue;
            List<LatestSensorValueResponse> points = values.values().stream()
                    .map(this::pipelineValue)
                    .filter(java.util.Objects::nonNull)
                    .filter(point -> point.measuredAt() != null && !point.measuredAt().isBefore(threshold))
                    .sorted(Comparator.comparing(LatestSensorValueResponse::sensorType,
                            Comparator.nullsLast(String::compareTo)))
                    .toList();
            if (!points.isEmpty()) result.put(ids.get(i), points);
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
                .filter(point -> point != null)
                .sorted(Comparator.comparing(LatestSensorValueResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }


    private boolean isNewer(LatestSensorValueResponse point, String existing) {
        LatestSensorValueResponse cached = deserialize(existing);
        return cached == null || point.measuredAt().isAfter(cached.measuredAt());
    }

    private String historyKey(long cultivationId, String deviceEui, String sensorType, String unit) {
        return HISTORY_PREFIX + cultivationId + ":" + deviceEui + ":" + sensorType + ":" + encodeUnit(unit);
    }

    private String latestField(LatestSensorValueResponse point) {
        return point.deviceEui() + "|" + point.sensorType() + "|" + encodeUnit(point.unit());
    }

    private String encodeUnit(String unit) {
        String normalized = SensorUnits.normalize(unit);
        if (normalized == null) {
            normalized = "<none>";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
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
