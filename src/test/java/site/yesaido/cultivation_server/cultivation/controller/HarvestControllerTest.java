package site.yesaido.cultivation_server.cultivation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.HarvestCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestCreateResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.HarvestDetailResponse;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;
import site.yesaido.cultivation_server.cultivation.service.impl.HarvestServiceImpl;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HarvestController.class)
class HarvestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HarvestServiceImpl harvestService;

    @Test
    @DisplayName("수확 기록 API - 정상 요청 시 201 Created 반환")
    void createHarvestSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestCreateRequest request = new HarvestCreateRequest(new BigDecimal("3.5"), "메모");
        HarvestCreateResponse response = new HarvestCreateResponse(200L, request.harvestWeight(), LocalDateTime.now(), null, null);

        when(harvestService.createHarvest(eq(cultivationId), eq(userId), any(HarvestCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cultivations/{cultivation-id}/harvest", cultivationId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.harvestId").value(200L));
    }

    @Test
    @DisplayName("수확 상세 조회 API - 정상 요청 시 200 OK 반환")
    void getHarvestSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        HarvestDetailResponse response = new HarvestDetailResponse(
                200L, cultivationId, new BigDecimal("3.5"), "테스트 버섯", LocalDateTime.now(), null, null
        );

        when(harvestService.getHarvest(cultivationId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/cultivations/{cultivation-id}/harvest", cultivationId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harvestId").value(200L))
                .andExpect(jsonPath("$.name").value("테스트 버섯"));
    }

    @Test
    @DisplayName("상품 점수 업데이트 API - 정상 요청 시 200 OK 반환")
    void updateProductScoreSuccess() throws Exception {
        Long userId = 1L;
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("95"));
        ProductScoreUpdateResponse response = new ProductScoreUpdateResponse(200L, request.productScore(), ProductGrade.TOP);

        when(harvestService.updateProductScore(eq(cultivationId), eq(userId), any(ProductScoreUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/cultivations/{cultivation-id}/harvest/product-score", cultivationId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productGrade").value(ProductGrade.TOP.name()));
    }
}
