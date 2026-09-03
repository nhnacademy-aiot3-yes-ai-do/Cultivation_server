package site.yesaido.cultivation_server.sensor.service;


import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.List;

public interface CultivationSensorService {
    CultivationSensor register(long cultivationId, CreateCultivationSensorRequest dto);

    CultivationSensorResponse findById(long cultivationId, long sensorId);

    void delete(long cultivationId, long sensorId);

    List<CultivationSensorResponse> findAll(long cultivationId);

    List<ReusableCultivationSensorResponse> findReusableSensors(Long userId, long excludedCultivationId);
}
