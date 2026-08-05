package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.SensorSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.List;
import java.util.Map;

public interface EnvironmentSettingService {

    void apply(long cultivationId, List<SensorSettingRequest> settings, Map<Long, SensorType> sensorTypes);
}
