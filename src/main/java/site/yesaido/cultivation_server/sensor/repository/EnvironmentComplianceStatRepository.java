package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentComplianceStat;

import java.util.List;
import java.util.Optional;

public interface EnvironmentComplianceStatRepository extends JpaRepository<EnvironmentComplianceStat, Long>, EnvironmentComplianceStatRepositoryCustom {
    Optional<EnvironmentComplianceStat> findByCultivationIdAndSensorType_Id(Long cultivationId, Long sensorTypeId);
    List<EnvironmentComplianceStat> findAllByCultivationId(Long cultivationId);


}
