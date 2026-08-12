package site.yesaido.cultivation_server.sensor.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class InfluxSensorQueryRepository {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final InfluxDBClient influxDBClient;
    private final InfluxProperties properties;

    public Long countTotal(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate) {
        String flux = baseFilter(cultivationId, sensorType, startDate, endDate)
                + "  |> group()\n"
                + "  |> count()";
        return executeCount(flux);
    }

    public long countInRange(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate,
                             BigDecimal thresholdMin, BigDecimal thresholdMax) {
        String flux = baseFilter(cultivationId, sensorType, startDate, endDate)
                + "  |> filter(fn: (r) => r._value >= " + thresholdMin + " and r._value <= " + thresholdMax + ")\n"
                + "  |> group()\n"
                + "  |> count()";
        return executeCount(flux);
    }

    private String baseFilter(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate) {
        ZonedDateTime start = startDate.atStartOfDay(ZONE);
        ZonedDateTime stop = endDate.plusDays(1).atStartOfDay(ZONE);

        return """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "%s" and r._field == "%s")
                  |> filter(fn: (r) => r.cultivationId == "%d" and r.sensorType == "%s")
                """.formatted(
                escape(properties.getBucket()),
                start.toInstant(), stop.toInstant(),
                SensorValuePointMapper.MEASUREMENT, SensorValuePointMapper.VALUE_FIELD,
                cultivationId, sensorType
        );
    }

    private long executeCount(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, properties.getOrg());
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(FluxRecord::getValue)
                .filter(Objects::nonNull)
                .mapToLong(value -> ((Number) value).longValue())
                .sum();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}