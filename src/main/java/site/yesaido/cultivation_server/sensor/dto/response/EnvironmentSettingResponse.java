package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;

import java.math.BigDecimal;

public record EnvironmentSettingResponse(
        Long sensorTypeId,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax
) {
    public static EnvironmentSettingResponse from(EnvironmentSetting setting) {
        return new EnvironmentSettingResponse(
                setting.getSensorType().getId(),
                setting.getThresholdMin(),
                setting.getThresholdMax()
        );
    }
}
