package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.EnvironmentSettingResponse;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.List;
import java.util.Map;

public interface EnvironmentSettingService {

    void apply(long cultivationId, List<EnvironmentSettingRequest> settings, Map<Long, SensorType> sensorTypes);

    void updateExisting(long cultivationId, EnvironmentSettingRequest request);

    List<EnvironmentSettingResponse> findAll(long cultivationId);
}
