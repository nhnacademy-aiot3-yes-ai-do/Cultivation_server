package site.yesaido.cultivation_server.dto.cultivation.response;


import site.yesaido.cultivation_server.dto.environmentsetting.RecommendedEnvironment;
import site.yesaido.cultivation_server.dto.sensor.response.RegisteredSensor;

import java.util.List;

public record CultivationCreateResponse(
        Long cultivationId,
        RecommendedEnvironment recommendedEnvironment,
        List<RegisteredSensor> registeredSensors
) {}
