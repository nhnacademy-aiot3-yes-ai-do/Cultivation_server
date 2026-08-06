package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentComplianceStat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnvironmentComplianceStatRepository extends JpaRepository<EnvironmentComplianceStat, Long>, EnvironmentComplianceStatRepositoryCustom {
    Optional<EnvironmentComplianceStat> findByCultivationIdAndSensorType_IdAndStatDate(Long cultivationId, Long sensorTypeId, LocalDate statDate);
    List<EnvironmentComplianceStat> findAllByCultivationIdAndStatDate(Long cultivationId, LocalDate statDate);

    // 누적 조회용
    List<EnvironmentComplianceStat> findAllByCultivationId(Long cultivationId);


}
