package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;

public interface CultivationSensorFacade {
    long register(Long userId, long cultivationId, CreateCultivationSensorRequest request);

    void delete(Long userId, long cultivationId, long sensorId);
}