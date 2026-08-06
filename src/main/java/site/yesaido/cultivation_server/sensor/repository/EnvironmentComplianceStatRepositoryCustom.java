package site.yesaido.cultivation_server.sensor.repository;

public interface EnvironmentComplianceStatRepositoryCustom {
    Long incrementInRange(Long cultivationId, Long sensorTypeId);
    Long incrementOutOfRange(Long cultivationId, Long sensorTypeId);
}
