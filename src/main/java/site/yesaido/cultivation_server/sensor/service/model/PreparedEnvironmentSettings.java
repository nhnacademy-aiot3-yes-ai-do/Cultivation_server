package site.yesaido.cultivation_server.sensor.service.model;

import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.List;
import java.util.Map;

public record PreparedEnvironmentSettings(
        List<EnvironmentSettingRequest> requests,
        Map<Long, SensorType> sensorTypeMap
) {
    public PreparedEnvironmentSettings {
        requests = List.copyOf(requests);
        sensorTypeMap = Map.copyOf(sensorTypeMap);
    }

}
