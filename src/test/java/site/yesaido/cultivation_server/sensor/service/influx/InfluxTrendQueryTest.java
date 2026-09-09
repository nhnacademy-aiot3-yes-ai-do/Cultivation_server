package site.yesaido.cultivation_server.sensor.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointResponse;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InfluxTrendQueryTest {

    @Test
    void returnsTwentyFourHourFifteenMinuteTrendForDeviceAndSensorType() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord fluxRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();
        Instant measuredAt = Instant.parse("2026-08-09T12:00:00Z");

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(fluxRecord));
        when(fluxRecord.getTime()).thenReturn(measuredAt);
        when(fluxRecord.getValue()).thenReturn(24.25);
        when(fluxRecord.getValues()).thenReturn(Map.of(
                "cultivationId", "42",
                "deviceEui", "eui-01",
                "sensorType", "TEMPERATURE",
                "unit", ".°C"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(
                client, properties
        );

        SensorTrendPointListResponse result = service.findTrend(
                42L, "eui-01", "TEMPERATURE", ".°C"
        );

        assertThat(result.unit()).isEqualTo(".°C");
        assertThat(result.responses())
                .containsExactly(new SensorTrendPointResponse(measuredAt, BigDecimal.valueOf(24.25)));
        verify(queryApi, times(8)).query(anyString(), eq("yes-nhn"));
        List<String> queries = mockingDetails(queryApi).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("query"))
                .map(invocation -> (String) invocation.getArguments()[0])
                .toList();
        assertThat(queries).allSatisfy(query -> assertThat(query)
                .contains("r.cultivationId == \"42\"")
                .contains("r.deviceEui == \"eui-01\"")
                .contains("r.sensorType == \"TEMPERATURE\"")
                .contains("r.unit == \".°C\""));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -9s)"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -59s, stop: -9s)")
                && query.contains("aggregateWindow(every: 3s"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -599s, stop: -59s)")
                && query.contains("aggregateWindow(every: 10s"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -1799s, stop: -599s)")
                && query.contains("aggregateWindow(every: 30s"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -3599s, stop: -1799s)")
                && query.contains("aggregateWindow(every: 1m"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -10799s, stop: -3599s)")
                && query.contains("aggregateWindow(every: 5m"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -21599s, stop: -10799s)")
                && query.contains("aggregateWindow(every: 10m"));
        assertThat(queries).anyMatch(query -> query.contains("range(start: -12h, stop: -21599s)")
                && query.contains("aggregateWindow(every: 20m"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}
