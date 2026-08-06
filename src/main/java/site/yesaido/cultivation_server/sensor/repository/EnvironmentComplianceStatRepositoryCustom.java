package site.yesaido.cultivation_server.sensor.repository;

import java.time.LocalDate;

public interface EnvironmentComplianceStatRepositoryCustom {
    Long incrementInRange(Long cultivationId, Long sensorTypeId, LocalDate statDate);
    Long incrementOutOfRange(Long cultivationId, Long sensorTypeId, LocalDate statDate);
}
