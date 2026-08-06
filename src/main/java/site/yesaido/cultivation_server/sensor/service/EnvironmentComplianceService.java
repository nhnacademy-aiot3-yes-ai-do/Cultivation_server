package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;

public interface EnvironmentComplianceService {
    void record(SensorValueEvent event);
    EnvironmentComplianceResponse getCompliance(Long cultivationId);
}
