package site.yesaido.cultivation_server.sensor.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.repository.InfluxSensorQueryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfluxSensorQueryRepositoryTest {

    @Mock
    InfluxDBClient client;

    @Mock
    QueryApi queryApi;

    @Mock
    InfluxProperties properties;

    @InjectMocks
    InfluxSensorQueryRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(client.getQueryApi()).thenReturn(queryApi);
        lenient().when(properties.getOrg()).thenReturn("yes-nhn");
        lenient().when(properties.getBucket()).thenReturn("sensor-data");
    }

    @Test
    void countTotalFiltersByCamelCaseTags() {
        FluxTable table = mock(FluxTable.class);
        FluxRecord testRecord = mock(FluxRecord.class);

        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(testRecord));
        when(testRecord.getValue()).thenReturn(10L);

        Long result = repository.countTotal(42L, "TEMPERATURE", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10));

        assertThat(result).isEqualTo(10L);
        verify(queryApi).query(argThat((String query) ->
                query.contains("r.cultivationId == \"42\"")
                        && query.contains("r.sensorType == \"TEMPERATURE\"")
                        && query.contains("|> group()")
                        && !query.contains("cultivation_id")
                        && !query.contains("sensor_type")
        ), eq("yes-nhn"));
    }

    @Test
    void countInRangeAddsThresholdFilter() {
        FluxTable table = mock(FluxTable.class);
        FluxRecord testRecord = mock(FluxRecord.class);

        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(testRecord));
        when(testRecord.getValue()).thenReturn(7L);

        long result = repository.countInRange(
                42L, "HUMIDITY", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70)
        );

        assertThat(result).isEqualTo(7L);
        verify(queryApi).query(argThat((String query) ->
                query.contains("r.sensorType == \"HUMIDITY\"")
                        && query.contains("r._value >= 40")
                        && query.contains("r._value <= 70")
                        && query.contains("|> group()")
        ), eq("yes-nhn"));
    }

    @Test
    void countInRangeSumsAcrossMultipleFluxTables() {
        FluxTable deviceATable = mock(FluxTable.class);
        FluxRecord deviceARecord = mock(FluxRecord.class);
        when(deviceARecord.getValue()).thenReturn(5L);
        when(deviceATable.getRecords()).thenReturn(List.of(deviceARecord));

        FluxTable deviceBTable = mock(FluxTable.class);
        FluxRecord deviceBRecord = mock(FluxRecord.class);
        when(deviceBRecord.getValue()).thenReturn(3L);
        when(deviceBTable.getRecords()).thenReturn(List.of(deviceBRecord));

        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(deviceATable, deviceBTable));

        long result = repository.countInRange(
                42L, "HUMIDITY", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70)
        );

        assertThat(result).isEqualTo(8L);
    }
}