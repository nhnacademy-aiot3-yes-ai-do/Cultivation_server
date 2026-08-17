package site.yesaido.cultivation_server.sensor.dto.response.datagenerator;

import site.yesaido.cultivation_server.sensor.entity.SensorType;

// Data Generator에 전달할 센서 타입과 단위 채널을 표현합니다.
public record DataGeneratorSensorTypeResponse(
        String sensorType,
        String unit
) {
    public static DataGeneratorSensorTypeResponse from(SensorType sensorType) {
        return new DataGeneratorSensorTypeResponse(
                sensorType.getType(),
                sensorType.getValueUnit()
        );
    }
}
