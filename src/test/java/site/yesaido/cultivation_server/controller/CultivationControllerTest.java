package site.yesaido.cultivation_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.dto.cultivation.response.*;
import site.yesaido.cultivation_server.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.entity.harvest.ProductGrade;
import site.yesaido.cultivation_server.service.CultivationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Test
    @DisplayName("재배 종료 API - 정상 요청 시 200 OK 반환")
    void finishCultivationSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        CultivationFinishResponse response = new CultivationFinishResponse(
                cultivationId,
                CultivationStatus.FINISHED,
                LocalDateTime.now()
        );

        when(cultivationService.finish(cultivationId, userId)).thenReturn(response);

        mockMvc.perform(put("/api/cultivations/{cultivation-id}/finish", cultivationId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cultivationId").value(cultivationId))
                .andExpect(jsonPath("$.status").value(CultivationStatus.FINISHED.name()));
    }

    @Test
    @DisplayName("재배 이력 조회 API - 200 OK 및 페이징된 내역 반환")
    void getHistorySuccess() throws Exception {
        Long userId = 1L;
        CultivationHistoryResponse history = new CultivationHistoryResponse(
                100L,
                "이전 버섯",
                1L,
                CultivationStatus.FINISHED,
                new BigDecimal(10.5),
                ProductGrade.TOP,
                LocalDateTime.now()
        );

        Page<CultivationHistoryResponse> historyPage = new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1);
        when(cultivationService.getHistory(eq(userId), any())).thenReturn(historyPage);

        mockMvc.perform(get("/api/cultivations/history")
                .header("X-User-Id", userId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cultivationId").value(100L))
                .andExpect(jsonPath("$.content[0].name").value("이전 버섯"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("재배 이력 조회 페이징 경계값 - 범위를 벗어난 페이지 요청 시 마지막 페이징 데이터를 반환")
    void getHistoryOutOfBoundsPage() throws Exception {
        Long userId = 1L;
        int outOfBoundsPage = 999;

        CultivationHistoryResponse historyResponse = new CultivationHistoryResponse(
                100L, "마지막 이력", 1L, CultivationStatus.FINISHED, new BigDecimal(10.5), ProductGrade.TOP, LocalDateTime.now()
        );

        Page<CultivationHistoryResponse> lastPage = new PageImpl<>(List.of(historyResponse), PageRequest.of(2, 20), 50);

        when(cultivationService.getHistory(eq(userId), any())).thenReturn(lastPage);

        mockMvc.perform(get("/api/cultivations/history")
                .header("X-User-Id", userId)
                .param("page", String.valueOf(outOfBoundsPage))
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[0].name").value("마지막 이력"))
                .andExpect(jsonPath("$.number").value(2));

    }
}
