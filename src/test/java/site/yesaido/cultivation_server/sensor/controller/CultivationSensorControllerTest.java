package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import site.yesaido.cultivation_server.sensor.dto.request.*;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CultivationSensorController.class)
class CultivationSensorControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CultivationSensorFacade cultivationSensorFacade;

    // 등록 성공
    @Test
    @DisplayName("센서 등록 성공 시 201 Created와 Location 헤더 반환")
    void registerSuccess() throws Exception {
        // given
        long userId = 1L;
        long cultivationId = 10L;
        long sensorId = 100L;

        SensorSettingRequest setting = new SensorSettingRequest(
                1L,
                new BigDecimal("18.0"),
                new BigDecimal("24.0")
        );

        CreateCultivationSensorRequest request =
                new CreateCultivationSensorRequest(
                        "EUI-001",
                        "MODEL-A",
                        "배양실 센서",
                        "ROOM-1",
                        "북쪽 선반",
                        List.of(setting)
                );

        given(cultivationSensorFacade.register(
                eq(userId),
                eq(cultivationId),
                any(CreateCultivationSensorRequest.class)
        )).willReturn(sensorId);

        // when
        mockMvc.perform(post(
                        "/api/cultivations/{cultivation-id}/sensors",
                        cultivationId
                )
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/cultivations/10/sensors/100"
                ));

        then(cultivationSensorFacade).should().register(
                eq(userId),
                eq(cultivationId),
                any(CreateCultivationSensorRequest.class)
        );
    }

    // 삭제 성공
    @Test
    @DisplayName("센서 삭제 성공 시 204 No Content 반환")
    void deleteSuccess() throws Exception {
        // given
        long userId = 1L;
        long cultivationId = 10L;
        long sensorId = 100L;

        // when
        mockMvc.perform(delete(
                "/api/cultivations/{cultivation-id}/sensors/{sensor-id}",
                        cultivationId,
                        sensorId
                )
                .header("X-User-Id", userId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        then(cultivationSensorFacade).should().delete(userId, cultivationId, sensorId);
    }

    // 등록 실패
    @Test
    @DisplayName("deviceEui가 공백이면 400 Bad Request 반환")
    void registerFailWhenDeviceEuiBlank() throws Exception {
        CreateCultivationSensorRequest request =
                new CreateCultivationSensorRequest(
                        "",
                        "MODEL-A",
                        "배양실 센서",
                        "ROOM-1",
                        "북쪽 선반",
                        List.of(new SensorSettingRequest(
                                1L,
                                new BigDecimal("18.0"),
                                new BigDecimal("24.0")
                        ))
                );

        mockMvc.perform(post(
                "/api/cultivations/{cultivation-id}/sensors",
                10L
                )
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(cultivationSensorFacade).shouldHaveNoInteractions();
    }



}