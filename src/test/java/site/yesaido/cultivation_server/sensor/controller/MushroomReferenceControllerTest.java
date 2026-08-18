package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceNotFoundException;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MushroomReferenceController.class)
class MushroomReferenceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MushroomReferenceService mushroomReferenceService;

    @Test
    @DisplayName("버섯 참조 모두 조회 API - 정상 요청시 200 ok 반환")
    void getAllMushroomReference() throws Exception {
        MushroomReferenceInfoResponse referenceInfoResponse1 = new MushroomReferenceInfoResponse(1L,
                "test-ko-name", "test-en-name", "test-scientific-name",
                List.of(), LocalDateTime.now(ZoneId.of("Asia/Seoul")), null);
        MushroomReferenceInfoResponse referenceInfoResponse2 = new MushroomReferenceInfoResponse(1L,
                "test-ko-name", "test-en-name", "test-scientific-name",
                List.of(), LocalDateTime.now(ZoneId.of("Asia/Seoul")), null);
        MushroomReferenceInfoListResponse response = new MushroomReferenceInfoListResponse(List.of(referenceInfoResponse1, referenceInfoResponse2));

        when(mushroomReferenceService.getAllMushroomReferenceInfoList()).thenReturn(response);

        String result = objectMapper.writeValueAsString(response);

        mockMvc.perform(get("/api/v1/mushroom-references"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(result));

        verify(mushroomReferenceService).getAllMushroomReferenceInfoList();
    }

}
