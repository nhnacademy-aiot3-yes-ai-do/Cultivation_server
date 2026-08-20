package site.yesaido.cultivation_server.sensor.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InfluxLatestQueryTest {

    @Test
    void returnsLatestReadingForEachSensorType() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord fluxRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(fluxRecord));
        when(fluxRecord.getValue()).thenReturn(23.5);
        when(fluxRecord.getTime()).thenReturn(Instant.parse("2026-08-09T12:34:56Z"));
        when(fluxRecord.getValues()).thenReturn(Map.of(
                "cultivationId", "42",
                "sensorType", "TEMPERATURE",
                "unit", "°C",
                "deviceEui", "eui-01",
                "deviceModel", "model-x",
                "deviceName", "sensor-01",
                "location", "room-1",
                "place", "farm-a"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(
                client, properties
        );

        LatestSensorValueListResponse result = service.findLatestByCultivationId(42L);

        assertThat(result.latestSensorValueResponses()).containsExactly(new LatestSensorValueResponse(
                42L, "TEMPERATURE", "°C", BigDecimal.valueOf(23.5),
                Instant.parse("2026-08-09T12:34:56Z"),
                "eui-01", "model-x", "sensor-01", "room-1", "farm-a"
        ));
        verify(queryApi).query(argThat((String query) ->
                query.contains("r.cultivationId == \"42\"")
                        && query.contains("r._field == \"value\"")
                        && query.contains("group(columns: [\"sensorType\", \"unit\", \"deviceEui\"])")
                        && query.contains("|> last()")
        ), eq("yes-nhn"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}
