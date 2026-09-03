package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.sensor.dto.response.influx.*;
import site.yesaido.cultivation_server.sensor.service.InfluxService;
import site.yesaido.cultivation_server.sensor.service.SensorRedisCacheService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorValueController.class)
class SensorValueControllerTest {

    private static final Long CULTIVATION_ID = 10L;
    private static final Long USER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    InfluxService influxService;

    @MockitoBean
    SensorRedisCacheService sensorRedisCacheService;

    @MockitoBean
    CultivationMemberService cultivationMemberService;


    @Test
    @DisplayName("최근 센서값 조회 성공 시 200 OK와 결과를 반환한다")
    void getLatestSuccess() throws Exception {
        List<LatestSensorValueResponse> latestSensorValueResponses = List.of(new LatestSensorValueResponse(
                CULTIVATION_ID, "TEMPERATURE", "C", new BigDecimal("22.5"), Instant.now(),
                "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반"
        ));
        LatestSensorValueListResponse response = new LatestSensorValueListResponse(latestSensorValueResponses);

        given(influxService.findLatestByCultivationId(CULTIVATION_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(cultivationMemberService).should().existCultivationMember(eq(CULTIVATION_ID), eq(USER_ID), isNull());
        then(influxService).should().findLatestByCultivationId(CULTIVATION_ID);
    }

    @Test
    @DisplayName("최신값 조회 시 Redis 값을 InfluxDB 결과와 병합한다")
    void getLatestUsesFreshRedisCache() throws Exception {
        LatestSensorValueResponse point = new LatestSensorValueResponse(
                CULTIVATION_ID, "TEMPERATURE", "C", new BigDecimal("22.5"), Instant.now(),
                "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반");
        given(sensorRedisCacheService.findLatest(eq(CULTIVATION_ID), any(java.time.Duration.class)))
                .willReturn(List.of(point));


        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());

        then(influxService).should().findLatestByCultivationId(CULTIVATION_ID);
    }

    @Test
    @DisplayName("동일 센서의 여러 unit 값을 모두 Redis cache에서 반환한다")
    void getLatestReturnsAllUnitsFromRedisCache() throws Exception {
        Instant measuredAt = Instant.now();
        LatestSensorValueResponse celsius = new LatestSensorValueResponse(
                CULTIVATION_ID, "TEMPERATURE", "°C", new BigDecimal("22.5"), measuredAt,
                "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반");
        LatestSensorValueResponse fahrenheit = new LatestSensorValueResponse(
                CULTIVATION_ID, "TEMPERATURE", "°F", new BigDecimal("72.5"), measuredAt,
                "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반");
        LatestSensorValueListResponse response =
                new LatestSensorValueListResponse(List.of(celsius, fahrenheit));
        given(sensorRedisCacheService.findLatest(eq(CULTIVATION_ID), any(java.time.Duration.class)))
                .willReturn(List.of(celsius, fahrenheit));

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(influxService).should().findLatestByCultivationId(CULTIVATION_ID);
    }

    @Test
    @DisplayName("Redis 최신값 조회 예외 시 InfluxDB로 fallback한다")
    void getLatestFallsBackWhenRedisFails() throws Exception {
        LatestSensorValueListResponse response = new LatestSensorValueListResponse(List.of());
        given(sensorRedisCacheService.findLatest(eq(CULTIVATION_ID), any(java.time.Duration.class)))
                .willThrow(new RuntimeException("redis unavailable"));
        given(influxService.findLatestByCultivationId(CULTIVATION_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());

        then(influxService).should().findLatestByCultivationId(CULTIVATION_ID);
    }


    @Test
    @DisplayName("재배 멤버가 아니면 최근 센서값 조회 없이 403을 반환한다")
    void getLatestFailsWhenNotMember() throws Exception {
        willThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .given(cultivationMemberService).existCultivationMember(eq(CULTIVATION_ID), eq(USER_ID), isNull());

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isForbidden());

        then(influxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최근 센서값 조회 - 관리자(X-User-Role=ADMIN)면 멤버가 아니어도 조회된다")
    void getLatestSuccessAdminRole() throws Exception {
        Long adminId = 999L;
        LatestSensorValueListResponse response = new LatestSensorValueListResponse(List.of());

        given(influxService.findLatestByCultivationId(CULTIVATION_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values", CULTIVATION_ID)
                        .header("X-User-Id", adminId)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        then(cultivationMemberService).should().existCultivationMember(eq(CULTIVATION_ID), eq(adminId), eq("ADMIN"));
    }

    @Test
    @DisplayName("센서 트렌드 조회 성공 시 200 OK와 결과를 반환한다")
    void getTrendSuccess() throws Exception {
        String deviceEui = "EUI-001";
        String sensorType = "TEMPERATURE";
        String unit = "C";
        SensorTrendPointListResponse response = new SensorTrendPointListResponse(
                CULTIVATION_ID, deviceEui, sensorType, "C",
                List.of(new SensorTrendPointResponse(Instant.now(), BigDecimal.valueOf(22.5)))
        );
        given(influxService.findTrend(CULTIVATION_ID, deviceEui, sensorType, unit)).willReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values/trend", CULTIVATION_ID)
                        .param("device-eui", deviceEui)
                        .param("sensor-type", sensorType)
                        .param("unit", unit)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(cultivationMemberService).should().existCultivationMember(CULTIVATION_ID, USER_ID);
        then(influxService).should().findTrend(CULTIVATION_ID, deviceEui, sensorType, unit);
    }

    @Test
    @DisplayName("trend Redis cache hit이면 InfluxDB를 호출하지 않는다")
    void getTrendUsesRedisCache() throws Exception {
        String deviceEui = "EUI-001";
        String sensorType = "TEMPERATURE";
        String unit = "C";
        SensorTrendPointListResponse response = new SensorTrendPointListResponse(
                CULTIVATION_ID, deviceEui, sensorType, unit,
                List.of(new SensorTrendPointResponse(Instant.now(), BigDecimal.valueOf(22.5))));
        given(sensorRedisCacheService.findTrend(CULTIVATION_ID, deviceEui, sensorType, unit))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values/trend", CULTIVATION_ID)
                        .param("device-eui", deviceEui)
                        .param("sensor-type", sensorType)
                        .param("unit", unit)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());

        then(influxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("device-eui 또는 sensor-type 파라미터가 없으면 400 Bad Request를 반환한다")
    void getTrendFailsWhenRequiredParamMissing() throws Exception {
        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values/trend", CULTIVATION_ID)
                        .param("device-eui", "EUI-001")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());

        then(influxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전체 센서 평균값 조회 성공 시 200 OK와 결과를 반환한다")
    void getAverageSuccess() throws Exception {
        List<SensorTypeAverageResponse> averages = List.of(
                new SensorTypeAverageResponse(CULTIVATION_ID, "TEMPERATURE", "°C", BigDecimal.valueOf(22.5)),
                new SensorTypeAverageResponse(CULTIVATION_ID, "HUMIDITY", "%", BigDecimal.valueOf(80.0))
        );
        SensorTypeAverageListResponse response = new SensorTypeAverageListResponse(averages);
        given(influxService.findAverageByCultivationId(CULTIVATION_ID)).willReturn(averages);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values/average", CULTIVATION_ID)
                .header("X-User-Id", USER_ID)).andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(cultivationMemberService).should().existCultivationMember(CULTIVATION_ID, USER_ID);
        then(influxService).should().findAverageByCultivationId(CULTIVATION_ID);
    }

    @Test
    @DisplayName("재배 멤버 아니면 센서 평균값 조회 없이 403 Forbidden 반환")
    void getAverageFailsWhenNotMember() throws Exception {
        willThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                .given(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID);
        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/sensor-values/average", CULTIVATION_ID)
                .header("X-User-Id", USER_ID)).andExpect(status().isForbidden());
        then(influxService).shouldHaveNoInteractions();
    }
}