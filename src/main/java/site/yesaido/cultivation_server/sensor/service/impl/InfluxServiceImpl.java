package site.yesaido.cultivation_server.sensor.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
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

    @Override
    public List<LatestSensorValueResponse> findLatestByCultivationId(long cultivationId) {

        String query = "from(bucket: \"" + escape(properties.getBucket()) + "\")"
                + " |> range(start: 0)"
                + " |> filter(fn: (r) => r._measurement == \"" + SensorValuePointMapper.MEASUREMENT + "\")"
                + " |> filter(fn: (r) => r._field == \"" + SensorValuePointMapper.VALUE_FIELD + "\")"
                + " |> filter(fn: (r) => r.cultivationId == \"" + cultivationId + "\")"
                + " |> group(columns: [\"sensorType\", \"unit\", \"deviceEui\"])"
                + " |> last()";

        return influxDBClient.getQueryApi()
                .query(query, properties.getOrg())
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

        String query = "from(bucket: \"" + escape(properties.getBucket()) + "\")"
                + " |> range(start: -24h)"
                + " |> filter(fn: (r) => r._measurement == \"" + SensorValuePointMapper.MEASUREMENT + "\")"
                + " |> filter(fn: (r) => r._field == \"" + SensorValuePointMapper.VALUE_FIELD + "\")"
                + " |> filter(fn: (r) => r.cultivationId == \"" + cultivationId + "\")"
                + " |> group(columns: [\"sensorType\", \"unit\"])"
                + " |> mean(column: \"_value\")";

        return influxDBClient.getQueryApi()
                .query(query, properties.getOrg())
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

        String query = "from(bucket: \"" + escape(properties.getBucket()) + "\")"
                + " |> range(start: -24h)"
                + " |> filter(fn: (r) => r._measurement == \"" + SensorValuePointMapper.MEASUREMENT + "\")"
                + " |> filter(fn: (r) => r._field == \"" + SensorValuePointMapper.VALUE_FIELD + "\")"
                + " |> filter(fn: (r) => r.cultivationId == \"" + cultivationId + "\")"
                + " |> filter(fn: (r) => r.deviceEui == \"" + escape(deviceEui) + "\")"
                + " |> filter(fn: (r) => r.sensorType == \"" + escape(sensorType) + "\")"
                + " |> aggregateWindow(every: 15m, fn: mean, createEmpty: false)"
                + " |> sort(columns: [\"_time\"])";

        List<FluxRecord> records = influxDBClient.getQueryApi()
                .query(query, properties.getOrg())
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
                .map(values -> stringValue(values, "sensorType"))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        String unit = records.stream()
                .map(FluxRecord::getValues)
                .map(values -> stringValue(values, "unit"))
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

    private long toCultivationId(List<FluxRecord>  records) {
        String cultivationIdFromInfluxDB = records.stream()
                .map(FluxRecord::getValues)
                .map(vaules -> stringValue(vaules, "cultivationId"))
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
                longValue(values, "cultivationId"),
                stringValue(values, "sensorType"),
                stringValue(values, "unit"),
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
                longValue(values, "cultivationId"),
                stringValue(values, "sensorType"),
                stringValue(values, "unit"),
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
}