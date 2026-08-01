package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;

public interface SensorTypeService {
    long registerSensorType(SensorTypeRequest dto);
    void updateSensorType(long sensorTypeId, SensorTypeRequest dto);
    void deleteSensorType(long sensorTypeId);

    SensorTypeInfoListResponse findAll();
}
