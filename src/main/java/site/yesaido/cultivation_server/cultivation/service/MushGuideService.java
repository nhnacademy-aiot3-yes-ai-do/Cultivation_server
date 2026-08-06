package site.yesaido.cultivation_server.cultivation.service;

import site.yesaido.cultivation_server.cultivation.dto.ai.MushGuideResponse;

public interface MushGuideService {
    MushGuideResponse getMushroomGuide(Long mushroomId);
}
