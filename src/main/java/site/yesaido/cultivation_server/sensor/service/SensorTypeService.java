package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.List;

public interface SensorTypeService {
    long registerSensorType(SensorTypeRequest dto);
    void updateSensorType(long sensorTypeId, SensorTypeRequest dto);
    void deleteSensorType(long sensorTypeId);

    SensorTypeInfoListResponse findAll();

    // 다른서비스에서 호출용
    List<SensorType> getSensorTypeList(List<Long> sensorTypeIds);
}
