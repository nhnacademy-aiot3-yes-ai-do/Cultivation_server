package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvironmentSettingController.class)
class EnvironmentSettingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CultivationSensorFacade cultivationSensorFacade;

    @Test
    @DisplayName("환경설정 수정 성공 시 204 No Content 반환")
    void updateSuccess() throws Exception {
        long userId = 1L;
        long cultivationId = 10L;
        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                3L,
                new BigDecimal("19.0"),
                new BigDecimal("25.0")
        );

        mockMvc.perform(put("/api/v1/cultivations/{cultivation-id}/environment-settings", cultivationId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        then(cultivationSensorFacade).should()
                .updateEnvironmentSetting(userId, cultivationId, request);
    }

    @Test
    @DisplayName("센서 타입 ID가 없으면 400 Bad Request 반환")
    void updateRejectsMissingSensorTypeId() throws Exception {
        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                null,
                new BigDecimal("19.0"),
                new BigDecimal("25.0")
        );

        mockMvc.perform(put("/api/v1/cultivations/{cultivation-id}/environment-settings", 10L)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(cultivationSensorFacade).shouldHaveNoInteractions();
    }
}
