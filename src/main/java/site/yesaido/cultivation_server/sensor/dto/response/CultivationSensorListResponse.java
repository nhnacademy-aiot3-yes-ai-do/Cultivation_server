package site.yesaido.cultivation_server.sensor.dto.response;

import java.util.List;

public record CultivationSensorListResponse(
        List<CultivationSensorResponse> sensors,
        List<EnvironmentSettingResponse> environmentSettings
) {
}
