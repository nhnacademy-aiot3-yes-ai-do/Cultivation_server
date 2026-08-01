package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.SensorType;

public record SensorTypeInfoResponse(
        long id,
        String type,
        String valueUnit
) {
    public static SensorTypeInfoResponse from(SensorType sensorType) {
        return new SensorTypeInfoResponse(sensorType.getId(), sensorType.getType(), sensorType.getValueUnit());
    }
}
