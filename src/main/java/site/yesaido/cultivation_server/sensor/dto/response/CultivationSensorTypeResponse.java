package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

public record CultivationSensorTypeResponse(
        Long sensorTypeId,
        String type,
        String valueUnit
) {
    public static CultivationSensorTypeResponse from(CultivationSensorType relation) {
        SensorType sensorType = relation.getSensorType();

        return new CultivationSensorTypeResponse(
                sensorType.getId(), sensorType.getType(), sensorType.getValueUnit()
        );
    }
}
