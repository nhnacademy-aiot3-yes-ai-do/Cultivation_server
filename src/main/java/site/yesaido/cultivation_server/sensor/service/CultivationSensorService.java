package site.yesaido.cultivation_server.sensor.service;


import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.List;

public interface CultivationSensorService {
    CultivationSensor register(long cultivationId, CreateCultivationSensorRequest dto);

    void delete(long cultivationId, long sensorId);

    List<CultivationSensorResponse> findAll(long cultivationId);
}
