package site.yesaido.cultivation_server.sensor.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InfluxSensorQueryRepositoryTest {

    @Test
    void countTotalFiltersByCamelCaseTags() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord testRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(testRecord));
        when(testRecord.getValue()).thenReturn(10L);

        InfluxSensorQueryRepository repository = new InfluxSensorQueryRepository(client, properties);

        Long result = repository.countTotal(42L, "TEMPERATURE", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10));

        assertThat(result).isEqualTo(10L);
        verify(queryApi).query(argThat((String query) ->
                query.contains("r.cultivationId == \"42\"")
                        && query.contains("r.sensorType == \"TEMPERATURE\"")
                        && !query.contains("cultivation_id")
                        && !query.contains("sensor_type")
        ), eq("yes-nhn"));
    }

    @Test
    void countInRangeAddsThresholdFilter() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord testRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(testRecord));
        when(testRecord.getValue()).thenReturn(7L);

        InfluxSensorQueryRepository repository = new InfluxSensorQueryRepository(client, properties);

        long result = repository.countInRange(
                42L, "HUMIDITY", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70)
        );

        assertThat(result).isEqualTo(7L);
        verify(queryApi).query(argThat((String query) ->
                query.contains("r.sensorType == \"HUMIDITY\"")
                        && query.contains("r._value >= 40")
                        && query.contains("r._value <= 70")
        ), eq("yes-nhn"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}