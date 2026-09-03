package site.yesaido.cultivation_server.sensor.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.NumberUtils;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.*;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.support.SensorUnits;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxServiceImpl implements InfluxService {

    private final InfluxDBClient influxDBClient;
    private final InfluxProperties properties;

    private static final String FIELD_SENSOR_TYPE = "sensorType";
    private static final String FIELD_CULTIVATION_ID = "cultivationId";
    private static final String FIELD_UNIT = "unit";

    @Override
    public List<LatestSensorValueResponse> findAveragedValuesByCultivationId(long cultivationId, java.time.Duration range) {
        long seconds = Math.max(1, range.toSeconds());
        List<LatestSensorValueResponse> points = new java.util.ArrayList<>();
        if (seconds <= 60) {
            points.addAll(queryAveragedValues(cultivationId, "-" + seconds + "s", null, "3s"));
        } else if (seconds <= 300) {
            points.addAll(queryAveragedValues(cultivationId, "-1m", null, "3s"));
            points.addAll(queryAveragedValues(cultivationId, "-" + seconds + "s", "-1m", "10s"));
        } else {
            points.addAll(queryAveragedValues(cultivationId, "-1m", null, "3s"));
            points.addAll(queryAveragedValues(cultivationId, "-5m", "-1m", "10s"));
            points.addAll(queryAveragedValues(cultivationId, "-" + Math.min(seconds, 3600) + "s", "-5m", "1m"));
            if (seconds > 3600) {
                points.addAll(queryAveragedValues(cultivationId, "-" + seconds + "s", "-1h", "5m"));
            }
        }
        return points;
    }

    @Override
    public List<LatestSensorValueResponse> findValuesByCultivationId(long cultivationId, java.time.Duration range) {
        String query = baseQuery(cultivationId, " |> range(start: -" + Math.max(1, range.toSeconds()) + "s)")
                + " |> filter(fn: (r) => exists r.deviceEui)"
                + " |> sort(columns: [\"_time\"] )";
        return queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toLatestSensorValue)
                .toList();
    }


    @Override
    public LatestSensorValueListResponse findLatestByCultivationId(long cultivationId) {

        String query = baseQuery(cultivationId, " |> range(start: 0)")
                + " |> group(columns: [\"sensorType\", \"unit\", \"deviceEui\"])"
                + " |> last()";

        List<LatestSensorValueResponse> list = queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toLatestSensorValue)
                .sorted(Comparator.comparing(
                        LatestSensorValueResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();

        if (list.isEmpty()) {
            log.debug(
                    "[InfluxDB] 센서 데이터 없음: cultivationId={}",
                    cultivationId
            );
        }

        return new LatestSensorValueListResponse(list);
    }

    @Override
    public Map<Long, List<LatestSensorValueResponse>> findLatestByCultivationIds(List<Long> cultivationIds) {
        List<Long> ids = cultivationIds == null ? List.of() : cultivationIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        String cultivationFilter = ids.stream()
                .map(id -> "r.cultivationId == \"" + id + "\"")
                .collect(java.util.stream.Collectors.joining(" or "));
        String query = "from(bucket: \"" + escape(properties.getBucket()) + "\")"
                + " |> range(start: 0)"
                + " |> filter(fn: (r) => r._measurement == \"" + SensorValuePointMapper.MEASUREMENT + "\")"
                + " |> filter(fn: (r) => r._field == \"" + SensorValuePointMapper.VALUE_FIELD + "\")"
                + " |> filter(fn: (r) => " + cultivationFilter + ")"
                + " |> group(columns: [\"cultivationId\", \"sensorType\", \"unit\", \"deviceEui\"] )"
                + " |> last()";

        Map<Long, List<LatestSensorValueResponse>> result = new LinkedHashMap<>();
        queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toLatestSensorValue)
                .filter(point -> point.cultivationId() != null)
                .forEach(point -> result.computeIfAbsent(point.cultivationId(), ignored -> new java.util.ArrayList<>()).add(point));
        return result;
    }

    @Override
    public List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId) {

        String query = baseQuery(cultivationId, " |> range(start: -24h)")
                + " |> group(columns: [\"sensorType\", \"unit\"])"
                + " |> mean(column: \"_value\")";

        return queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toSensorTypeAverage)
                .sorted(Comparator.comparing(
                        SensorTypeAverageResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();
    }

    @Override
    public SensorTrendPointListResponse findTrend(
            long cultivationId,
            String deviceEui,
            String sensorType,
            String unit
    ) {
        Objects.requireNonNull(deviceEui, "deviceEui must not be null");
        Objects.requireNonNull(sensorType, "sensorType must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        String normalizedUnit = Objects.requireNonNull(SensorUnits.normalize(unit), "unit must not be blank");

        String query = baseQuery(cultivationId, " |> range(start: -12h)")
                + " |> filter(fn: (r) => r.deviceEui == \"" + escape(deviceEui) + "\")"
                + " |> filter(fn: (r) => r.sensorType == \"" + escape(sensorType) + "\")"
                + " |> filter(fn: (r) => r.unit == \"" + escape(normalizedUnit) + "\")"
                + " |> aggregateWindow(every: 15m, fn: mean, createEmpty: false)"
                + " |> sort(columns: [\"_time\"])";

        List<FluxRecord> records = queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .toList();

        if (records.isEmpty()) {
            return new SensorTrendPointListResponse(cultivationId, deviceEui, sensorType, normalizedUnit, List.of());
        }

        long cultivationIdFromInfluxDB = toCultivationId(records);

        String deviceEuiFromDB = records.stream()
                .map(FluxRecord::getValues)
                .map(values -> stringValue(values, "deviceEui"))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);


        String sensorTypeFromDB = records.stream()
                .map(FluxRecord::getValues)
                .map(values -> stringValue(values, FIELD_SENSOR_TYPE))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        String responseUnit = records.stream()
                .map(FluxRecord::getValues)
                .map(values -> stringValue(values, FIELD_UNIT))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        List<SensorTrendPointResponse> responses = records.stream()
                .map(fluxRecord -> {
                    Object rawValue = fluxRecord.getValue();
                    if (!(rawValue instanceof Number number)) {
                        throw new IllegalStateException("Influx trend value is not numeric: " + rawValue);
                    }
                    return new SensorTrendPointResponse(fluxRecord.getTime(), NumberUtils.convertNumberToTargetClass(number, BigDecimal.class));
                })
                .toList();

        return new SensorTrendPointListResponse(
                cultivationIdFromInfluxDB, deviceEuiFromDB, sensorTypeFromDB, responseUnit, responses
        );
    }

    @Override
    public List<SensorTypeAverageResponse> findAverageByCultivationId(long cultivationId) {
        String query = baseQuery(cultivationId, " |> range(start: 0)")
                + " |> group(columns: [\"sensorType\", \"unit\"])"
                + " |> mean(column: \"_value\")";

        return queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toSensorTypeAverage)
                .sorted(Comparator.comparing(
                        SensorTypeAverageResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();
    }

    private long toCultivationId(List<FluxRecord> records) {
        String cultivationIdFromInfluxDB = records.stream()
                .map(FluxRecord::getValues)
                .map(vaules -> stringValue(vaules, FIELD_CULTIVATION_ID))
                .filter(Objects::nonNull).findFirst().orElse(null);

        return Long.parseLong(Objects.requireNonNull(cultivationIdFromInfluxDB));
    }

    private List<LatestSensorValueResponse> queryAveragedValues(
            long cultivationId,
            String start,
            String stop,
            String every
    ) {
        String range = stop == null
                ? " |> range(start: " + start + ")"
                : " |> range(start: " + start + ", stop: " + stop + ")";
        String query = baseQuery(cultivationId, range)
                + " |> filter(fn: (r) => exists r.deviceEui)"
                + " |> group(columns: [\"deviceEui\", \"sensorType\", \"unit\"])"
                + " |> aggregateWindow(every: " + every + ", fn: mean, createEmpty: false)"
                + " |> sort(columns: [\"_time\"] )";
        return queryTablesSafely(query).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toLatestSensorValue)
                .toList();
    }

    private SensorTypeAverageResponse toSensorTypeAverage(FluxRecord fluxRecord) {
        Object rawValue = fluxRecord.getValue();
        if (!(rawValue instanceof Number number)) {
            throw new IllegalStateException("Influx average value is not numeric: " + rawValue);
        }

        Map<String, Object> values = fluxRecord.getValues();
        return new SensorTypeAverageResponse(
                longValue(values, FIELD_CULTIVATION_ID),
                stringValue(values, FIELD_SENSOR_TYPE),
                stringValue(values, FIELD_UNIT),
                NumberUtils.convertNumberToTargetClass(number, BigDecimal.class)
        );
    }

    private LatestSensorValueResponse toLatestSensorValue(FluxRecord fluxRecord) {
        Map<String, Object> values = fluxRecord.getValues();
        Object rawValue = fluxRecord.getValue();
        if (!(rawValue instanceof Number number)) {
            throw new IllegalStateException("Influx sensor value is not numeric: " + rawValue);
        }

        return new LatestSensorValueResponse(
                longValue(values, FIELD_CULTIVATION_ID),
                stringValue(values, FIELD_SENSOR_TYPE),
                stringValue(values, FIELD_UNIT),
                NumberUtils.convertNumberToTargetClass(number, BigDecimal.class),
                fluxRecord.getTime(),
                stringValue(values, "deviceEui"),
                stringValue(values, "deviceModel"),
                stringValue(values, "deviceName"),
                stringValue(values, "location"),
                stringValue(values, "place")
        );
    }

    private Long longValue(Map<String, Object> values, String key) {
        String value = stringValue(values, key);
        return value == null ? null : Long.valueOf(value);
    }

    private String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<FluxTable> queryTablesSafely(String query) {
        try {
            return influxDBClient.getQueryApi().query(query, properties.getOrg());
        } catch (InfluxException e) {
            String logContent = """
                InfluxDB 센서 데이터 조회 실패
                url=%s
                org=%s
                bucket=%s
                cause=%s
                """.formatted(
                    properties.getUrl(),
                    properties.getOrg(),
                    properties.getBucket(),
                    e.getMessage()
            );

            throw new CustomServerException(
                    "센서 데이터를 조회하는 중 오류가 발생했습니다.",
                    logContent,
                    e,
                    ServerErrorLevel.ERROR_LEVEL
            );
        }
    }

    private String baseQuery(long cultivationId, String rangeClause) {
        return "from(bucket: \"" + escape(properties.getBucket()) + "\")"
                + rangeClause
                + " |> filter(fn: (r) => r._measurement == \"" + SensorValuePointMapper.MEASUREMENT + "\")"
                + " |> filter(fn: (r) => r._field == \"" + SensorValuePointMapper.VALUE_FIELD + "\")"
                + " |> filter(fn: (r) => r.cultivationId == \"" + cultivationId + "\")";
    }
}