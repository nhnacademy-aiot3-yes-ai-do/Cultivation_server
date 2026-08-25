package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.util.Optional;

public interface SensorTypeRepository extends JpaRepository<SensorType, Long> {
    boolean existsSensorTypeByTypeAndValueUnit(String type, String valueUnit);

    boolean existsSensorTypeById(Long id);

    @Query("""
            SELECT CASE WHEN COUNT(sensorType) > 0 THEN true ELSE false END
            FROM SensorType sensorType
            WHERE sensorType.id = :sensorTypeId
              AND (
                  EXISTS (SELECT cultivationSensorType.id FROM CultivationSensorType cultivationSensorType
                          WHERE cultivationSensorType.sensorType = sensorType)
                  OR EXISTS (SELECT mushroomReferenceThreshold.id FROM MushroomReferenceThreshold mushroomReferenceThreshold
                             WHERE mushroomReferenceThreshold.sensorType = sensorType)
                  OR EXISTS (SELECT environmentSetting.id FROM EnvironmentSetting environmentSetting
                             WHERE environmentSetting.sensorType = sensorType)
              )
            """)
    boolean existsInUseById(@Param("sensorTypeId") Long sensorTypeId);

    SensorType findSensorTypeById(Long id);

    Optional<SensorType> findByType(String type);
}
