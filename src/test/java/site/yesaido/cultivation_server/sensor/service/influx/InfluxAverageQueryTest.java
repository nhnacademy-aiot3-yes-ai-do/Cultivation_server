package site.yesaido.cultivation_server.sensor.service.influx;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.SensorTypeAverageResponse;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;

import java.math.BigDecimal;
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
        FluxRecord fluxRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(fluxRecord));
        when(fluxRecord.getValue()).thenReturn(23.75);
        when(fluxRecord.getValues()).thenReturn(Map.of(
                "cultivationId", "42",
                "sensorType", "TEMPERATURE",
                "unit", "C"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(
                client, properties
        );

        List<SensorTypeAverageResponse> result =
                service.findAverageByCultivationIdForLast24Hours(42L);

        assertThat(result).containsExactly(
                new SensorTypeAverageResponse(42L, "TEMPERATURE", "C", BigDecimal.valueOf(23.75))
        );

        verify(queryApi).query(argThat((String query) ->
                query.contains("range(start: -24h)")
                        && query.contains("r.cultivationId == \"42\"")
                        && query.contains("r._field == \"value\"")
                        && query.contains(
                        "group(columns: [\"sensorType\", \"unit\"])"
                )
                        && query.contains("|> mean(column: \"_value\")")
                        && !query.contains("deviceEui")
        ), eq("yes-nhn"));
    }

    @Test
    @DisplayName("재배기의 전체 누적 센서 데이터 평균을 계산하여 반환한다")
    void returnsAverageForEachSensorTypeForTotalPeriod() {
        // Given
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        FluxTable table = mock(FluxTable.class);
        FluxRecord fluxRecord = mock(FluxRecord.class);
        InfluxProperties properties = properties();

        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(anyString(), eq("yes-nhn"))).thenReturn(List.of(table));
        when(table.getRecords()).thenReturn(List.of(fluxRecord));
        when(fluxRecord.getValue()).thenReturn(21.50);
        when(fluxRecord.getValues()).thenReturn(Map.of(
                "cultivationId", "42",
                "sensorType", "TEMPERATURE",
                "unit", "°C"
        ));

        InfluxServiceImpl service = new InfluxServiceImpl(client, properties);

        List<SensorTypeAverageResponse> result = service.findAverageByCultivationId(42L);

        assertThat(result).containsExactly(
                new SensorTypeAverageResponse(42L, "TEMPERATURE", "°C", BigDecimal.valueOf(21.50))
        );

        verify(queryApi).query(argThat((String query) ->
                query.contains("range(start: 0)")
                        && query.contains("r.cultivationId == \"42\"")
                        && query.contains("group(columns: [\"sensorType\", \"unit\"])")
                        && query.contains("|> mean(column: \"_value\")")
        ), eq("yes-nhn"));
    }

    private InfluxProperties properties() {
        InfluxProperties properties = new InfluxProperties();
        properties.setOrg("yes-nhn");
        properties.setBucket("sensor-data");
        return properties;
    }
}
