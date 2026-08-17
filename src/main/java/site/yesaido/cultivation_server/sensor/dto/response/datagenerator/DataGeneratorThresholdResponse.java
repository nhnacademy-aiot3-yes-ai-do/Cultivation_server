package site.yesaido.cultivation_server.sensor.dto.response.datagenerator;

import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;

import java.math.BigDecimal;

// Data Generator에 전달할 재배별 센서 채널의 임계값을 표현합니다.
public record DataGeneratorThresholdResponse(
        long cultivationId,
        String sensorType,
        String unit,
        BigDecimal minValue,
        BigDecimal maxValue
) {

    public static DataGeneratorThresholdResponse from(
            EnvironmentSetting environmentSetting
    ) {
        return new DataGeneratorThresholdResponse(
                environmentSetting.getCultivationId(),
                environmentSetting.getSensorType().getType(),
                environmentSetting.getSensorType().getValueUnit(),
                environmentSetting.getThresholdMin(),
                environmentSetting.getThresholdMax()
        );
    }
}