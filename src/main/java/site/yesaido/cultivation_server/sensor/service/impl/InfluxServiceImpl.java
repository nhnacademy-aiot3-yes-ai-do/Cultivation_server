package site.yesaido.cultivation_server.sensor.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.NumberUtils;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;
import site.yesaido.cultivation_server.sensor.service.InfluxService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class InfluxServiceImpl implements InfluxService {

    private final InfluxDBClient influxDBClient;
    private final InfluxProperties properties;

    private static final String FIELD_SENSOR_TYPE = "sensorType";
    private static final String FIELD_CULTIVATION_ID = "cultivationId";
    private static final String FIELD_UNIT = "unit";

    @Override
    public List<LatestSensorValueResponse> findLatestByCultivationId(long cultivationId) {

        String query = baseQuery(cultivationId, " |> range(start: 0)")
                + " |> group(columns: [\"sensorType\", \"unit\", \"deviceEui\"])"
                + " |> last()";

        return queryTablesSafely(query)
                .stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toLatestSensorValue)
                .sorted(Comparator.comparing(
                        LatestSensorValueResponse::sensorType,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();
    }

    @Override
    public List<SensorTypeAverageResponse> findAverageByCultivationIdForLast24Hours(long cultivationId) {

        String query = baseQuery(cultivationId, " |> range(start: -24h)")
                + " |> group(columns: [\"sensorType\", \"unit\"])"
                + " |> mean(column: \"_value\")";

        return queryTablesSafely(query)
                .stream()
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
            String sensorType
    ) {
        Objects.requireNonNull(deviceEui, "deviceEui must not be null");
        Objects.requireNonNull(sensorType, "sensorType must not be null");

        String query = baseQuery(cultivationId, " |> range(start: -24h)")
                + " |> filter(fn: (r) => r.deviceEui == \"" + escape(deviceEui) + "\")"
                + " |> filter(fn: (r) => r.sensorType == \"" + escape(sensorType) + "\")"
                + " |> aggregateWindow(every: 15m, fn: mean, createEmpty: false)"
                + " |> sort(columns: [\"_time\"])";

        List<FluxRecord> records = queryTablesSafely(query)
                .stream()
                .flatMap(table -> table.getRecords().stream())
                .toList();

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

        String unit = records.stream()
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
                    return new SensorTrendPointResponse(fluxRecord.getTime(), number.doubleValue());
                })
                .toList();

        return new SensorTrendPointListResponse(
                cultivationIdFromInfluxDB, deviceEuiFromDB, sensorTypeFromDB, unit, responses
        );
    }

    private long toCultivationId(List<FluxRecord> records) {
        String cultivationIdFromInfluxDB = records.stream()
                .map(FluxRecord::getValues)
                .map(vaules -> stringValue(vaules, FIELD_CULTIVATION_ID))
                .filter(Objects::nonNull).findFirst().orElse(null);

        return Long.parseLong(Objects.requireNonNull(cultivationIdFromInfluxDB));
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
                number.doubleValue()
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
            if (e.getMessage() != null && e.getMessage().contains("FluxTable definition was not found")) {
                return List.of();
            }
            throw e;
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