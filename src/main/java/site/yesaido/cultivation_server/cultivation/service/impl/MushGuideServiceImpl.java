package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.yesaido.cultivation_server.cultivation.client.AiClient;
import site.yesaido.cultivation_server.cultivation.dto.ai.ApiResponse;
import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;
import site.yesaido.cultivation_server.cultivation.service.MushGuideService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MushGuideServiceImpl implements MushGuideService {
    private final AiClient aiClient;

    @Override
    public MushGuideResponse getMushroomGuide(Long mushroomId) {
        ApiResponse<MushGuideResponse> apiResponse = aiClient.getMushroomGuide(mushroomId);
        return apiResponse.data();
    }
}
