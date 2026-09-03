package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CultivationSensorRepository extends JpaRepository<CultivationSensor, Long> {

    Optional<CultivationSensor> findByCultivationIdAndDeviceEui(long cultivationId, String deviceEui);

    // 경작에 등록된 센서를 센서 Id, 경작 Id로 조회
    @EntityGraph(attributePaths = {
            "cultivationSensorTypes",
            "cultivationSensorTypes.sensorType"
    })
    Optional<CultivationSensor> findByIdAndCultivationIdAndIsDeletedFalse(Long sensorId, long cultivationId);

    // 등록된 센서 경작Id, DeviceEui로 조회
    Optional<CultivationSensor> findByCultivationIdAndDeviceEuiAndIsDeletedFalse(long cultivationId, String deviceEui);

    // 경작 Id로 등록된 모든 센서 조회
    @EntityGraph(attributePaths = {
            "cultivationSensorTypes",
            "cultivationSensorTypes.sensorType"
    })
    List<CultivationSensor> findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(long cultivationId);

    @Query("""
              SELECT DISTINCT cultivationSensor
              FROM CultivationSensor cultivationSensor
              LEFT JOIN FETCH cultivationSensor.cultivationSensorTypes cultivationSensorType
              LEFT JOIN FETCH cultivationSensorType.sensorType
              WHERE cultivationSensor.cultivationId <> :excludedCultivationId
                AND cultivationSensor.cultivationId IN (
                    SELECT cultivation.id
                    FROM Cultivation cultivation
                    WHERE cultivation.userId = :userId
                )
              ORDER BY cultivationSensor.createdAt DESC, cultivationSensor.id DESC
              """)
    List<CultivationSensor> findReusableSensorsForOwner(
            @Param("userId") Long userId,
            @Param("excludedCultivationId") long excludedCultivationId
    );

    // 활성 재배의 삭제되지 않은 센서와 측정 채널을 snapshot으로 조회합니다.
    @Query("""
              SELECT DISTINCT cultivationSensor
              FROM CultivationSensor cultivationSensor
              LEFT JOIN FETCH cultivationSensor.cultivationSensorTypes cultivationSensorType
              LEFT JOIN FETCH cultivationSensorType.sensorType
              WHERE cultivationSensor.isDeleted = false
                AND cultivationSensor.cultivationId IN (
                    SELECT cultivation.id
                    FROM Cultivation cultivation
                    WHERE cultivation.cultivationStatus IN :activeStatuses
                )
              ORDER BY cultivationSensor.cultivationId ASC,
                       cultivationSensor.deviceEui ASC
              """)
    List<CultivationSensor> findAllForDataGeneratorSnapshot(@Param("activeStatuses") Collection<CultivationStatus> activeStatuses);
}
