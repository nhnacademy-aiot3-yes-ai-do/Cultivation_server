package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.List;
import java.util.Optional;

public interface CultivationSensorRepository extends JpaRepository<CultivationSensor, Long> {

    Optional<CultivationSensor> findByCultivationIdAndDeviceEui(long cultivationId, String deviceEui);

    // 경작에 등록된 센서를 센서 Id, 경작 Id로 조회
    Optional<CultivationSensor> findByIdAndCultivationIdAndIsDeletedFalse(Long sensorId, long cultivationId);

    // 등록된 센서 경작Id, DeviceEui로 조회
    Optional<CultivationSensor> findByCultivationIdAndDeviceEuiAndIsDeletedFalse(long cultivationId, String deviceEui);

    // 경작 Id로 등록된 모든 센서 조회
    List<CultivationSensor> findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(long cultivationId);
}
