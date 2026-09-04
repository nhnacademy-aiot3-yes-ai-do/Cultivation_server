package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.dto.projection.CultivationSensorEuiProjection;
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

    /**
     * 사용자가 접근할 수 있는 재배지의 삭제되지 않은 센서 EUI를 한 번에 조회합니다.
     * cultivation_member는 접근 권한, cultivation_sensor는 센서 등록 관계를 담당합니다.
     */
    @Query(value = """
            SELECT DISTINCT cs.cultivation_id AS cultivationId,
                            cs.device_eui AS deviceEui
            FROM cultivation_sensor cs
            INNER JOIN cultivation_member cm
                    ON cm.cultivation_id = cs.cultivation_id
            WHERE cm.user_id = :userId
              AND cs.is_deleted = false
            ORDER BY cs.cultivation_id ASC, cs.device_eui ASC
            """, nativeQuery = true)
    List<CultivationSensorEuiProjection> findAllAccessibleSensorEuis(
            @Param("userId") Long userId
    );

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
