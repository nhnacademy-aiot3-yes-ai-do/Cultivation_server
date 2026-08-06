package site.yesaido.cultivation_server.cultivation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;
import site.yesaido.cultivation_server.cultivation.service.MushGuideService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MushGuideController.class)
class MushGuideControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private MushGuideService mushGuideService;

    @Test
    @DisplayName("버섯 가이드 조회 API - 정상 요청시 200 OK 반환 및 JSON 반환")
    void getMushGuideSuccess() throws Exception {
        Long mushroomId = 1L;
        MushGuideResponse response = new MushGuideResponse( // 가짜 응답 생성
                mushroomId,
                "느타리버섯",
                null,
                "느타리버섯 요약 정보",
                "건조 주의",
                "자주 물주기",
                null,
                null,
                List.of()
        );

        when(mushGuideService.getMushroomGuide(mushroomId)).thenReturn(response);

        mockMvc.perform(get("/api/mushrooms/{mushroom-id}/guide", mushroomId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 200 OK 상태코드 검증
                .andExpect(jsonPath("$.mushroomId").value(1L)) // JSON 데이터의 mushroomId 값 검증
                .andExpect(jsonPath("$.mushroomName").value("느타리버섯"))
                .andExpect(jsonPath("$.summary").value("느타리버섯 요약 정보"));
    }

    @Test
    @DisplayName("AI 서버 통신 실패 시 503 Service Unavailable 반환")
    void getMushGuideFeignExceptionTest() throws Exception {
        Long mushroomId = 1L;

        // AI 서버가 503 에러를 보냈다는 상황을 가상으로 구성
        feign.FeignException feignException = feign.FeignException.errorStatus(
                "getMushroomGuide",
                feign.Response.builder().status(503)
                .request(feign.Request.create(feign.Request.HttpMethod.GET, "", java.util.Map.of(), null, null, null)).build()
        );
        // Service 메서드를 부르면 위에서 만든 503 FeignException이 터지도록 설정
        when(mushGuideService.getMushroomGuide(mushroomId)).thenThrow(feignException);

        mockMvc.perform(get("/api/mushrooms/{mushroom-id}/guide", mushroomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable()); // 503 상태코드 검증
    }



}
