package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.Collection;
import java.util.List;

// 한 물리 센서가 온도·습도·CO2 등 여러 타입을 가질 때 사용하는 연결 테이블
public interface CultivationSensorTypeRepository extends JpaRepository<CultivationSensorType, Long> {

    List<CultivationSensorType> findAllByCultivationSensor_Id(Long cultivationSensorId);

    boolean existsByCultivationSensor_IdAndSensorType_Id(Long cultivationSensorId, Long sensorTypeId);

    void deleteAllByCultivationSensor_IdAndSensorType_IdIn(Long cultivationSensorId, Collection<Long> sensorTypeIds);

    List<CultivationSensorType> findAllByCultivationSensor_IdAndSensorType_IdIn(Long cultivationSensorId, Collection<Long> sensorTypeIds);

    // 재배지에 등록된 (삭제되지 않은) 물리 센서들이 갖고 있는 센서 타입 조회
    @Query("""
                SELECT DISTINCT cst.sensorType
                FROM CultivationSensorType cst
                WHERE cst.cultivationSensor.cultivationId = :cultivationId
                AND cst.cultivationSensor.isDeleted = false
                """)
    List<SensorType> findDistinctSensorTypesByCultivationId(@Param("cultivationId")long cultivationId);
}
