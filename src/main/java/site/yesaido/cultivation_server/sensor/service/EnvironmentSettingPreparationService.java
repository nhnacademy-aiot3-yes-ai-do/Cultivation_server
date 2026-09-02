package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.service.model.PreparedEnvironmentSettings;

import java.util.List;

public interface EnvironmentSettingPreparationService {
    PreparedEnvironmentSettings prepare(
            List<EnvironmentSettingRequest> requests
    );
}
