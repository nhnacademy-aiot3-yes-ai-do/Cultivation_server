package site.yesaido.cultivation_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationDetailResponse;
import site.yesaido.cultivation_server.dto.cultivation.response.CultivationSummaryResponse;
import site.yesaido.cultivation_server.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.service.CultivationService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CultivationController.class)
class CultivationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CultivationService cultivationService;

    @Test
    @DisplayName("경작 생성 API - 정상 요청시 201 Created 반환")
    void createCultivationSuccess() throws Exception {
        Long userId = 1L;
        CultivationCreateRequest request = new CultivationCreateRequest("테스트 버섯", 1L, Collections.emptyList());

        CultivationCreateResponse response = new CultivationCreateResponse(100L, null, Collections.emptyList());

        when(cultivationService.create(any(CultivationCreateRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/cultivations")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cultivationId").value(100L));
    }

    @Test
    @DisplayName("경작 목록 조회 API - 정상 요청 시 200 OK 반환")
    void getCultivationSuccess() throws Exception {
        Long userId = 1L;
        CultivationSummaryResponse summary = new CultivationSummaryResponse(100L, "테스트 버섯", 1L, CultivationStatus.CREATED, CultivationMode.GROWTH, LocalDateTime.now());

        when(cultivationService.getCultivations(userId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/cultivations")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cultivationId").value(100L))
                .andExpect(jsonPath("$[0].name").value("테스트 버섯"));
    }

    @Test
    @DisplayName("단일 경작 상세 조회 API - 정상 요청 시 200 OK 반환")
    void getCultivationDetailSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        CultivationDetailResponse detail = new CultivationDetailResponse(
                cultivationId,
                "테스트 버섯",
                1L,
                CultivationStatus.CREATED,
                CultivationMode.GROWTH,
                LocalDateTime.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(cultivationService.getCultivation(userId, cultivationId)).thenReturn(detail);

        mockMvc.perform(get("/api/cultivations/{cultivation-id}", cultivationId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cultivationId").value(cultivationId))
                .andExpect(jsonPath("$.name").value("테스트 버섯"));
    }
}
