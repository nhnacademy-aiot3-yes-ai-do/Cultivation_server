package site.yesaido.cultivation_server.sensor.service.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InfluxAverageQueryTest {

    @Test
    void returnsAverageForEachSensorTypeDuringLast24Hours() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord record = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(record));
        when(record.getValue()).thenReturn(23.75);
        when(record.getValues()).thenReturn(Map.of(
                "cultivationId", "42",
                "sensorType", "TEMPERATURE",
                "unit", "°C"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(
                client, properties, new SensorValuePointMapper()
        );

        List<SensorTypeAverageResponse> result =
                service.findAverageByCultivationIdForLast24Hours(42L);

        assertThat(result).containsExactly(
                new SensorTypeAverageResponse(42L, "TEMPERATURE", "°C", 23.75)
        );
        verify(queryApi).query(argThat((String query) ->
                query.contains("range(start: -24h)")
                        && query.contains("r.cultivationId == \"42\"")
                        && query.contains("r._field == \"value\"")
                        && query.contains("group(columns: [\"sensorType\", \"unit\"])")
                        && query.contains("|> mean(column: \"_value\")")
                        && !query.contains("deviceEui")
        ), eq("yes-nhn"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}
