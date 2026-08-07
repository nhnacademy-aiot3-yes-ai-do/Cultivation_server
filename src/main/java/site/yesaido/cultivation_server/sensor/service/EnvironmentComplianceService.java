package site.yesaido.cultivation_server.sensor.service;

import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;

import java.time.LocalDate;

public interface EnvironmentComplianceService {
    EnvironmentComplianceResponse getCompliance(Long cultivationId);
    EnvironmentComplianceResponse getDailyCompliance(Long cultivationId, LocalDate date);
    EnvironmentComplianceResponse getComplianceForPeriod(Long cultivationId, LocalDate startDate, LocalDate endDate);
}
