package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 경작지별 센서 타입당 설정 한 건 관리
public interface EnvironmentSettingRepository extends JpaRepository<EnvironmentSetting, Long> {
    // 각 경작의 한 센서 타입 가져오기
    // 문제 -> 순회조회할때마다 쿼리문 실행
    Optional<EnvironmentSetting> findByCultivationIdAndSensorType_Id(long cultivationId, Long sensorTypeId);

    List<EnvironmentSetting> findAllByCultivationIdAndSensorType_IdIn(long cultivationId, Collection<Long> sensorTypeIds);

    // 경작 Id에 등록된 모든 타입 센서 조회
    List<EnvironmentSetting> findAllByCultivationId(long cultivationId);

    // 활성 재배의 모든 센서 임계값을 SensorType과 함께 snapshot으로 조회합니다.
    @Query("""
              SELECT environmentSetting
              FROM EnvironmentSetting environmentSetting
              JOIN FETCH environmentSetting.sensorType sensorType
              WHERE environmentSetting.cultivationId IN (
                  SELECT cultivation.id
                  FROM Cultivation cultivation
                  WHERE cultivation.cultivationStatus IN :activeStatuses
              )
              ORDER BY environmentSetting.cultivationId ASC,
                       sensorType.type ASC,
                       sensorType.valueUnit ASC
              """)
    List<EnvironmentSetting> findAllForDataGeneratorSnapshot(@Param("activeStatuses") Collection<CultivationStatus> activeStatuses);
}
