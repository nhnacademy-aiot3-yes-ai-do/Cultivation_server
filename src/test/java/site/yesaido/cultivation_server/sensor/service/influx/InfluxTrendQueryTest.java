package site.yesaido.cultivation_server.sensor.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.rabbitmq.event.SensorType;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTrendPointResponse;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;

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
                "unit", "°C"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(
                client, properties, new SensorValuePointMapper()
        );

        SensorTrendPointListResponse result = service.findTrend(
                42L, "eui-01", SensorType.TEMPERATURE
        );

        assertThat(result.unit()).isEqualTo("°C");
        assertThat(result.responses())
                .containsExactly(new SensorTrendPointResponse(measuredAt, 24.25));
        verify(queryApi).query(argThat((String query) ->
                query.contains("range(start: -24h)")
                        && query.contains("r.cultivationId == \"42\"")
                        && query.contains("r.deviceEui == \"eui-01\"")
                        && query.contains("r.sensorType == \"TEMPERATURE\"")
                        && query.contains("aggregateWindow(every: 15m, fn: mean")
                        && query.contains("createEmpty: false")
        ), eq("yes-nhn"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}
