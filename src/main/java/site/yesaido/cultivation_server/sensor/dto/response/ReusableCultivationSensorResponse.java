package site.yesaido.cultivation_server.sensor.dto.response;

import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.Comparator;
import java.util.List;

public record ReusableCultivationSensorResponse(
        Long sourceCultivationId,
        String deviceEui,
        String deviceModel,
        String deviceName,
        String location,
        String locationDetail,
        List<CultivationSensorTypeResponse> sensorTypes
) {
    public ReusableCultivationSensorResponse {
        sensorTypes = sensorTypes == null ? List.of() : List.copyOf(sensorTypes);
    }

    public static ReusableCultivationSensorResponse from(CultivationSensor sensor) {
        List<CultivationSensorTypeResponse> types = sensor.getCultivationSensorTypes().stream()
                .map(CultivationSensorTypeResponse::from)
                .sorted(Comparator.comparing(CultivationSensorTypeResponse::sensorTypeId))
                .toList();

        return new ReusableCultivationSensorResponse(
                sensor.getCultivationId(),
                sensor.getDeviceEui(),
                sensor.getDeviceModel(),
                sensor.getDeviceName(),
                sensor.getLocation(),
                sensor.getLocationDetail(),
                types
        );
    }
}
