package site.yesaido.cultivation_server.cultivation.controller.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.DailyCultivationPhotoListResponse;
import site.yesaido.cultivation_server.cultivation.dto.cultivationphoto.DailyCultivationPhotoResponse;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationPhotoServiceImpl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CultivationPhotoInternalController.class)
class CultivationPhotoInternalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CultivationPhotoServiceImpl cultivationPhotoService;

    @Test
    @DisplayName("일별 사진 조회 API - 정상 요청 시 200 OK와 사진 목록을 반환한다")
    void getDailyPhotosSuccess() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 28);
        DailyCultivationPhotoResponse photo = new DailyCultivationPhotoResponse(
                100L, 500L, "http://storage.java21.net:8000/team2-mushroom-photos/objectKey", OffsetDateTime.now()
        );
        DailyCultivationPhotoListResponse response = new DailyCultivationPhotoListResponse(targetDate, List.of(photo));

        when(cultivationPhotoService.getDailyPhotos(targetDate)).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/cultivations/photos/daily")
                        .param("date", "2026-08-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2026-08-28"))
                .andExpect(jsonPath("$.photos[0].cultivationId").value(100L))
                .andExpect(jsonPath("$.photos[0].photoId").value(500L));
    }

    @Test
    @DisplayName("일별 사진 조회 API - 해당 날짜에 사진이 없으면 빈 목록과 함께 200 OK를 반환한다")
    void getDailyPhotosSuccessEmpty() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 28);
        DailyCultivationPhotoListResponse response = new DailyCultivationPhotoListResponse(targetDate, List.of());

        when(cultivationPhotoService.getDailyPhotos(targetDate)).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/cultivations/photos/daily")
                        .param("date", "2026-08-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos").isEmpty());
    }

    @Test
    @DisplayName("일별 사진 조회 API 실패 - date 파라미터 누락 시 400 Bad Request")
    void getDailyPhotosFailMissingDateParam() throws Exception {
        mockMvc.perform(get("/api/v1/internal/cultivations/photos/daily"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일별 사진 조회 API 실패 - date 형식이 잘못되면 500 Internal Server Error (타입 변환 실패는 GlobalExceptionHandler의 catch-all에서 처리됨)")
    void getDailyPhotosFailInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/internal/cultivations/photos/daily")
                        .param("date", "2026/08/28"))
                .andExpect(status().isInternalServerError());
    }
}