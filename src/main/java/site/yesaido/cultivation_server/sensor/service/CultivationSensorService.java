package site.yesaido.cultivation_server.sensor.service;


import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

public interface CultivationSensorService {
    CultivationSensor register(long cultivationId, CreateCultivationSensorRequest dto);

    void delete(long cultivationId, long sensorId);
}
