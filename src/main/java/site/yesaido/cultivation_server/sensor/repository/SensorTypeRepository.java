package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

public interface SensorTypeRepository extends JpaRepository<SensorType, Long> {
    boolean existsSensorTypeByTypeAndValueUnit(String type, String valueUnit);

    boolean existsSensorTypeById(Long id);

    SensorType findSensorTypeById(Long id);
}
