package site.yesaido.cultivation_server.sensor.repository.sensortype;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

public interface SensorTypeRepository extends JpaRepository<SensorType, Long> {
}
