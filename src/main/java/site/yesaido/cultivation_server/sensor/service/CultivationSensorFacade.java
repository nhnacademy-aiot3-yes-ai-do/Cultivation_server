package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;

public interface CultivationSensorFacade {
    long register(Long userId, long cultivationId, CreateCultivationSensorRequest request);

    void delete(Long userId, long cultivationId, long sensorId);

    CultivationSensorListResponse findAll(Long userId, long cultivationId);
}