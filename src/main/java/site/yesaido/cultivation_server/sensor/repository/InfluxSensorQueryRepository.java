package site.yesaido.cultivation_server.sensor.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public class InfluxSensorQueryRepository {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final InfluxDBClient influxDBClient;
    private final String bucket;

    public InfluxSensorQueryRepository(InfluxDBClient influxDBClient, @Value("${influx.bucket}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
    }

    public Long countTotal(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate) {
        String flux = baseFilter(cultivationId, sensorType, startDate, endDate) + "  |> count()";
        return executeCount(flux);
    }

    public long countInRange(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate,
                             BigDecimal thresholdMin, BigDecimal thresholdMax) {
        String flux = baseFilter(cultivationId, sensorType, startDate, endDate)
                + "  |> filter(fn: (r) => r._value >= " + thresholdMin + " and r._value <= " + thresholdMax + ")\n"
                + "  |> count()";
        return executeCount(flux);
    }

    private String baseFilter(Long cultivationId, String sensorType, LocalDate startDate, LocalDate endDate) {
        ZonedDateTime start = startDate.atStartOfDay(ZONE);
        ZonedDateTime stop = endDate.plusDays(1).atStartOfDay(ZONE);

        return """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "sensor_value" and r._field == "value")
                  |> filter(fn: (r) => r.cultivation_id == "%d" and r.sensor_type == "%s")
                """.formatted(bucket, start.toInstant(), stop.toInstant(), cultivationId, sensorType);
    }

    private long executeCount(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
            return 0L;
        }
        Object value = tables.get(0).getRecords().get(0).getValue();
        return value == null ? 0L : ((Number) value).longValue();
    }
}
