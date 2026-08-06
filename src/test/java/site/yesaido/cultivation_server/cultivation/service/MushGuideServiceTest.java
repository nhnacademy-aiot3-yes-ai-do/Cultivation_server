package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.yesaido.cultivation_server.cultivation.client.AiClient;
import site.yesaido.cultivation_server.cultivation.dto.ai.ApiResponse;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;
import site.yesaido.cultivation_server.cultivation.service.impl.MushGuideServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MushGuideServiceTest {
    @Mock
    private AiClient aiClient;

    @InjectMocks
    private MushGuideServiceImpl mushGuideService;

    @Test
    @DisplayName("버섯 가이드 조회 성공")
    void getMushGuideSuccess() {
        Long mushroomId = 1L;
        MushGuideResponse mockGuide = new MushGuideResponse(
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
        ApiResponse<MushGuideResponse> apiResponse = ApiResponse.success(mockGuide);

        when(aiClient.getMushroomGuide(mushroomId)).thenReturn(apiResponse);
        MushGuideResponse result = mushGuideService.getMushroomGuide(mushroomId);

        assertThat(result).isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(mockGuide); // mockGuide의 모든 필드 값과 result의 모든 필드 값이 같은지 통째로 비교

        verify(aiClient).getMushroomGuide(mushroomId);
    }


}
