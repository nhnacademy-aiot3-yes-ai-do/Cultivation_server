package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorFacade;

@WebMvcTest(CultivationSensorController.class)
class CultivationSensorControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CultivationSensorFacade cultivationSensorFacade;

    @Test
    void registerSuccess() throws Exception {

    }



}