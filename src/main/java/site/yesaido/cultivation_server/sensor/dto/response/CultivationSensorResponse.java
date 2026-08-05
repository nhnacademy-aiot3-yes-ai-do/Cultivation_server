package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;

import java.util.Comparator;
import java.util.List;

public record CultivationSensorResponse(
        Long sensorId,
        String deviceEui,
        String deviceModel,
        String deviceName,
        String location,
        String locationDetail,
        SensorConnectStatus sensorStatus,
        List<CultivationSensorTypeResponse> sensorTypes
) {
    public static CultivationSensorResponse from(
            CultivationSensor sensor
    ) {
        List<CultivationSensorTypeResponse> sensorTypes =
                sensor.getCultivationSensorTypes().stream()
                        .map(CultivationSensorTypeResponse::from)
                        .sorted(Comparator.comparing(
                                CultivationSensorTypeResponse::sensorTypeId
                        ))
                        .toList();

        return new CultivationSensorResponse(
                sensor.getId(),
                sensor.getDeviceEui(),
                sensor.getDeviceModel(),
                sensor.getDeviceName(),
                sensor.getLocation(),
                sensor.getLocationDetail(),
                sensor.getSensorStatus(),
                sensorTypes
        );
    }
}
