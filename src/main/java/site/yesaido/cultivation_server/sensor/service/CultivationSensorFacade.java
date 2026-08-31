package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;

public interface CultivationSensorFacade {
    long register(Long userId, long cultivationId, CreateCultivationSensorRequest request);

    void updateEnvironmentSetting(Long userId, long cultivationId, EnvironmentSettingRequest request);

    void delete(Long userId, long cultivationId, long sensorId);

    // 경작 종료후 수확 호출시 경작 끝 처리한후 호출, harvest에서 owner 체크하므로 권한체크 불필요
    void deleteAll(Long userId, long cultivationId);

    CultivationSensorListResponse findAll(Long userId, long cultivationId);
    CultivationSensorListResponse findAll(Long userId, long cultivationId, String role);
}
