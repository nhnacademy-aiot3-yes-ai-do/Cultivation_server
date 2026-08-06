package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;

import java.time.LocalDate;

public interface EnvironmentComplianceService {
    void recordCount(SensorValueEvent event);
    EnvironmentComplianceResponse getCompliance(Long cultivationId);
    EnvironmentComplianceResponse getDailyCompliance(Long cultivationId, LocalDate date);
}
