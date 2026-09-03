package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserSensorCatalogController.class)
class UserSensorCatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CultivationSensorFacade cultivationSensorFacade;

    @Test
    @DisplayName("소유한 기존 센서 목록을 대상 경작지를 제외하고 조회한다")
    void getReusableSensors() throws Exception {
        ReusableCultivationSensorResponse sensor = new ReusableCultivationSensorResponse(
                4L,
                "EUI-001",
                "MODEL-A",
                "온습도 센서",
                "광주",
                "1번 선반",
                List.of()
        );
        given(cultivationSensorFacade.findReusableSensors(7L, 99L))
                .willReturn(new ReusableCultivationSensorListResponse(List.of(sensor)));

        mockMvc.perform(get("/api/v1/sensors/reusable")
                        .header("X-User-Id", 7L)
                        .param("exclude-cultivation-id", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors[0].sourceCultivationId").value(4L))
                .andExpect(jsonPath("$.sensors[0].deviceEui").value("EUI-001"));

        then(cultivationSensorFacade).should().findReusableSensors(7L, 99L);
    }
}
