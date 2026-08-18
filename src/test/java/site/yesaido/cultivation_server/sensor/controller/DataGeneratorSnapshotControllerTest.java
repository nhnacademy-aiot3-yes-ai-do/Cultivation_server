package site.yesaido.cultivation_server.sensor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorSnapshotResponse;
import site.yesaido.cultivation_server.sensor.service.DataGeneratorSnapshotService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataGeneratorSnapshotController.class)
class DataGeneratorSnapshotControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DataGeneratorSnapshotService dataGeneratorSnapshotService;

    @Test
    @DisplayName("snapshot 조회 성공 시 200 OK와 서비스 응답을 그대로 반환한다")
    void getSnapshotSuccess() throws Exception {
        DataGeneratorSnapshotResponse response = new DataGeneratorSnapshotResponse(
                OffsetDateTime.parse("2026-08-17T00:00:00+09:00"),
                List.of(),
                List.of()
        );
        given(dataGeneratorSnapshotService.getSnapshot()).willReturn(response);

        mockMvc.perform(get("/api/v1/internal/data-generator/snapshot"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        then(dataGeneratorSnapshotService).should().getSnapshot();
    }
}