package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;

import java.util.Collection;
import java.util.List;

// 한 물리 센서가 온도·습도·CO2 등 여러 타입을 가질 때 사용하는 연결 테이블
public interface CultivationSensorTypeRepository extends JpaRepository<CultivationSensorType, Long> {

    List<CultivationSensorType> findAllByCultivationSensor_Id(Long cultivationSensorId);

    boolean existsByCultivationSensor_IdAndSensorType_Id(Long cultivationSensorId, Long sensorTypeId);

    void deleteAllByCultivationSensor_IdAndSensorType_IdIn(Long cultivationSensorId, Collection<Long> sensorTypeIds);

    List<CultivationSensorType> findAllByCultivationSensor_IdAndSensorType_IdIn(Long cultivationSensorId, Collection<Long> sensorTypeIds);
}
