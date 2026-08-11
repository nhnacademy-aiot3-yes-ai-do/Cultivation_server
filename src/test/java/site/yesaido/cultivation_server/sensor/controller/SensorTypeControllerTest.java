package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorTypeController.class)
class SensorTypeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SensorTypeService sensorTypeService;

    @Test
    @DisplayName("센서 모두 조회 API - 정상 요청시 200 ok 반환")
    void getAllSensorType() throws Exception {
        SensorTypeInfoResponse stir1 = new SensorTypeInfoResponse(1L, "test-type-1", "test-unit-1");
        SensorTypeInfoResponse stir2 = new SensorTypeInfoResponse(2L, "test-type-2", "test-unit-2");
        SensorTypeInfoListResponse response = new SensorTypeInfoListResponse(List.of(stir1, stir2));

        when(sensorTypeService.findAll()).thenReturn(response);

        String result = objectMapper.writeValueAsString(response);

        mockMvc.perform(get("/api/v1/sensor-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(result));

        verify(sensorTypeService).findAll();
    }
}
