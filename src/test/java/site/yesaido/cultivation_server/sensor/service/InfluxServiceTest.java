package site.yesaido.cultivation_server.sensor.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;
import site.yesaido.cultivation_server.config.InfluxProperties;
import site.yesaido.cultivation_server.sensor.dto.response.influx.*;
import site.yesaido.cultivation_server.sensor.service.impl.InfluxServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfluxServiceTest {

    private static final long CULTIVATION_ID = 1L;
    private static final String INFLUX_URL =
            "http://influxdb.influxdb.svc.cluster.local:8086";
    private static final String INFLUX_ORG = "yes-nhn";
    private static final String INFLUX_BUCKET = "sensor-data";

    @Mock
    InfluxDBClient influxDBClient;

    @Mock
    QueryApi queryApi;

    @Mock
    InfluxProperties influxProperties;

    @InjectMocks
    InfluxServiceImpl influxService;

    @BeforeEach
    void setUp() {
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(influxProperties.getOrg()).thenReturn(INFLUX_ORG);
        when(influxProperties.getBucket()).thenReturn(INFLUX_BUCKET);
    }

    @Nested
    @DisplayName("최근 센서값 조회")
    class FindLatest {

        @Test
        @DisplayName("저장된 센서값이 없는 케이스: 빈 목록을 반환")
        void returnsEmptyListWhenSensorValuesDoNotExist() {
            // given
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of());

            // when
            LatestSensorValueListResponse response =
                    influxService.findLatestByCultivationId(CULTIVATION_ID);

            // then
            assertThat(response.latestSensorValueResponses()).isEmpty();

            verify(queryApi).query(anyString(), eq(INFLUX_ORG));
        }

        @Test
        @DisplayName("InfluxDB 조회 실패시 서버 예외 발생")
        void throwsCustomServerExceptionWhenInfluxQueryFails() {
            // given
            InfluxException influxException =
                    new InfluxException("connection refused");

            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenThrow(influxException);

            when(influxProperties.getUrl()).thenReturn(INFLUX_URL);

            // when: 인자 순서를 Class, Lambda 순으로 변경
            CustomServerException exception = catchThrowableOfType(
                    CustomServerException.class,
                    () -> influxService.findLatestByCultivationId(CULTIVATION_ID)
            );

            // then
            assertThat(exception)
                    .isNotNull()
                    .hasMessage("센서 데이터를 조회하는 중 오류가 발생했습니다.");

            assertThat(exception.getErrorLevel())
                    .isEqualTo(ServerErrorLevel.ERROR_LEVEL);

            assertThat(exception.getCause())
                    .isSameAs(influxException);

            assertThat(exception.getLogContent())
                    .contains(
                            "InfluxDB 센서 데이터 조회 실패",
                            "url=" + INFLUX_URL,
                            "org=" + INFLUX_ORG,
                            "bucket=" + INFLUX_BUCKET,
                            "connection refused"
                    );

            verify(queryApi, times(1))
                    .query(anyString(), eq(INFLUX_ORG));
        }

        @Test
        @DisplayName("센서 타입 오름차순으로 정렬하고 타입이 없는 값은 마지막에 배치")
        void returnsLatestValuesSortedBySensorTypeWithNullLast() {
            Instant measuredAt = Instant.parse("2026-08-24T00:00:00Z");

            FluxRecord temperature = toRecord(
                    23.5,
                    measuredAt,
                    Map.of(
                            "cultivationId", "1",
                            "sensorType", "TEMPERATURE",
                            "unit", "C",
                            "deviceEui", "eui-01"
                    )
            );

            Map<String, Object> valuesWithoutSensorType = new HashMap<>();
            valuesWithoutSensorType.put("unit", "UNKNOWN");
            FluxRecord unknown = toRecord(10, measuredAt, valuesWithoutSensorType);
            FluxTable table = tableOf(unknown, temperature);

            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            LatestSensorValueListResponse response =
                    influxService.findLatestByCultivationId(CULTIVATION_ID);

            assertThat(response.latestSensorValueResponses())
                    .extracting(LatestSensorValueResponse::sensorType)
                    .containsExactly("TEMPERATURE", null);
            assertThat(response.latestSensorValueResponses().getFirst().value())
                    .isEqualByComparingTo(BigDecimal.valueOf(23.5));
            assertThat(response.latestSensorValueResponses().getLast().cultivationId())
                    .isNull();
        }

        @Test
        @DisplayName("최근 센서값이 숫자가 아니면 예외 발생")
        void rejectsNonNumericLatestValue() {
            FluxRecord recordRow = toRecord("not-a-number", null, Map.of());
            FluxTable table = tableOf(recordRow);
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            assertThatThrownBy(() ->
                    influxService.findLatestByCultivationId(CULTIVATION_ID)
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Influx sensor value is not numeric");
        }
    }

    @Nested
    @DisplayName("24시간 평균 조회")
    class FindAverage {

        @Test
        @DisplayName("센서 타입과 단위별 평균값 반환")
        void returnsAverageBySensorTypeAndUnit() {
            FluxRecord recordRow = toRecord(
                    61.25,
                    null,
                    Map.of(
                            "cultivationId", "1",
                            "sensorType", "HUMIDITY",
                            "unit", "%"
                    )
            );
            FluxTable table = tableOf(recordRow);
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            List<SensorTypeAverageResponse> response =
                    influxService.findAverageByCultivationIdForLast24Hours(CULTIVATION_ID);

            assertThat(response).containsExactly(
                    new SensorTypeAverageResponse(
                            CULTIVATION_ID,
                            "HUMIDITY",
                            "%",
                            BigDecimal.valueOf(61.25)
                    )
            );
        }

        @Test
        @DisplayName("평균값이 숫자가 아니면 예외 발생")
        void rejectsNonNumericAverageValue() {
            FluxRecord recordRow = mock(FluxRecord.class);
            when(recordRow.getValue()).thenReturn("invalid");
            FluxTable table = tableOf(recordRow);
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            assertThatThrownBy(() ->
                    influxService.findAverageByCultivationIdForLast24Hours(CULTIVATION_ID)
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Influx average value is not numeric");
        }
    }

    @Nested
    @DisplayName("24시간 추이 조회")
    class FindTrend {

        @Test
        @DisplayName("15분 단위 센서 추이 반환")
        void returnsTrendPoints() {
            Instant measuredAt = Instant.parse("2026-08-24T01:15:00Z");
            FluxRecord recordRow = toRecord(
                    24.75,
                    measuredAt,
                    Map.of(
                            "cultivationId", "1",
                            "deviceEui", "eui-01",
                            "sensorType", "TEMPERATURE",
                            "unit", "C"
                    )
            );
            FluxTable table = tableOf(recordRow);
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            SensorTrendPointListResponse response = influxService.findTrend(
                    CULTIVATION_ID,
                    "eui-01",
                    "TEMPERATURE"
            );

            assertThat(response.cultivationId()).isEqualTo(CULTIVATION_ID);
            assertThat(response.deviceEui()).isEqualTo("eui-01");
            assertThat(response.sensorType()).isEqualTo("TEMPERATURE");
            assertThat(response.unit()).isEqualTo("C");
            assertThat(response.responses()).containsExactly(
                    new SensorTrendPointResponse(
                            measuredAt,
                            BigDecimal.valueOf(24.75)
                    )
            );
        }

        @Test
        @DisplayName("추이값이 숫자가 아니면 예외 발생")
        void rejectsNonNumericTrendValue() {
            FluxRecord recordRow = toRecord(
                    "invalid",
                    null,
                    Map.of(
                            "cultivationId", "1",
                            "deviceEui", "eui-01",
                            "sensorType", "TEMPERATURE",
                            "unit", "C"
                    )
            );
            FluxTable table = tableOf(recordRow);
            when(queryApi.query(anyString(), eq(INFLUX_ORG)))
                    .thenReturn(List.of(table));

            assertThatThrownBy(() -> influxService.findTrend(
                    CULTIVATION_ID,
                    "eui-01",
                    "TEMPERATURE"
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Influx trend value is not numeric");
        }
    }

    private FluxTable tableOf(FluxRecord... records) {
        FluxTable table = mock(FluxTable.class);
        when(table.getRecords()).thenReturn(List.of(records));
        return table;
    }

    private FluxRecord toRecord(
            Object value,
            Instant measuredAt,
            Map<String, Object> values
    ) {
        FluxRecord recordRow = mock(FluxRecord.class);
        when(recordRow.getValue()).thenReturn(value);
        when(recordRow.getValues()).thenReturn(values);
        if (measuredAt != null) {
            when(recordRow.getTime()).thenReturn(measuredAt);
        }
        return recordRow;
    }
}
