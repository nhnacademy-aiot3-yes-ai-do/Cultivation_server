package site.yesaido.cultivation_server.cultivation.controller.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.harvest.request.ProductScoreUpdateRequest;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.ProductScoreUpdateResponse;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;
import site.yesaido.cultivation_server.cultivation.service.impl.HarvestServiceImpl;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HarvestInternalController.class)
class HarvestInternalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HarvestServiceImpl harvestService;

    @Test
    @DisplayName("상품 점수 업데이트 내부 API - X-User-Id 없이도 200 OK 반환")
    void updateProductScoreSuccess() throws Exception {
        Long cultivationId = 100L;
        ProductScoreUpdateRequest request = new ProductScoreUpdateRequest(new BigDecimal("95"));
        ProductScoreUpdateResponse response = new ProductScoreUpdateResponse(200L, request.productScore(), ProductGrade.TOP);

        when(harvestService.updateProductScoreInternal(eq(cultivationId), any(ProductScoreUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/internal/cultivations/{cultivation-id}/harvest/product-score", cultivationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productGrade").value(ProductGrade.TOP.name()));
    }
}