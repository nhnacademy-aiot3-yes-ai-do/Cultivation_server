package site.yesaido.cultivation_server.sensor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
