package site.yesaido.cultivation_server.sensor.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    @DisplayName("센서 타입 등록 API - 정상 요청시 201 created 반환")
    void registerSensorType() throws Exception {
        SensorTypeRequest sensorTypeRequest = new SensorTypeRequest("test-type", "test-unit");
        Long sensorTypeId = 1L;

        when(sensorTypeService.registerSensorType(any(SensorTypeRequest.class))).thenReturn(sensorTypeId);

        String s = objectMapper.writeValueAsString(sensorTypeRequest);

        mockMvc.perform(post("/api/v1/sensor-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(s))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/1")));
    }
}
