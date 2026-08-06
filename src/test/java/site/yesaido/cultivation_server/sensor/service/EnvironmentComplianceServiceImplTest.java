package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentComplianceStat;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentComplianceStatRepository;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.repository.SensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.impl.EnvironmentComplianceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EnvironmentComplianceServiceImplTest {

    private static final long CULTIVATION_ID = 10L;
    private static final long SENSOR_TYPE_ID = 1L;

    @Mock
    EnvironmentComplianceStatRepository environmentComplianceStatRepository;

    @Mock
    EnvironmentSettingRepository environmentSettingRepository;

    @Mock
    SensorTypeRepository sensorTypeRepository;

    @InjectMocks
    EnvironmentComplianceServiceImpl service;

    private SensorType temperatureType() {
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        ReflectionTestUtils.setField(sensorType, "id", SENSOR_TYPE_ID);
        return sensorType;
    }

    private SensorValueEvent event(double value) {
        return new SensorValueEvent(
                "배양실", "ROOM-1", "MODEL-A", "온도센서1", "EUI-001",
                site.yesaido.cultivation_server.rabbitmq.event.SensorType.TEMPERATURE,
                value, LocalDateTime.now(), CULTIVATION_ID
        );
    }
    @Test
    @DisplayName("매핑되는 센서 타입이 없으면 아무 것도 하지 않음")
    void doesNothing_whenSensorTypeUnknown() {
        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.empty());

        service.recordCount(event(25.0));

        then(environmentSettingRepository).shouldHaveNoInteractions();
        then(environmentComplianceStatRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("해당 경작에 임계값 설정이 없으면 아무 것도 하지 않음")
    void doesNothing_whenNoEnvironmentSetting() {
        SensorType sensorType = temperatureType();
        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.empty());

        service.recordCount(event(25.0));

        then(environmentComplianceStatRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("값이 임계값 범위 안이면 incrementInRange 호출")
    void incrementsInRange_whenValueWithinThreshold() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.empty());

        service.recordCount(event(25.0));

        then(environmentComplianceStatRepository).should().save(any(EnvironmentComplianceStat.class));
        then(environmentComplianceStatRepository).should().incrementInRange(CULTIVATION_ID, SENSOR_TYPE_ID);
        then(environmentComplianceStatRepository).should(never()).incrementOutOfRange(CULTIVATION_ID, SENSOR_TYPE_ID);
    }

    @Test
    @DisplayName("값이 임계값 범위 밖이면 incrementOutOfRange 호출")
    void incrementsOutOfRange_whenValueOutsideThreshold() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.empty());

        service.recordCount(event(35.0));

        then(environmentComplianceStatRepository).should().incrementOutOfRange(CULTIVATION_ID, SENSOR_TYPE_ID);
        then(environmentComplianceStatRepository).should(never()).incrementInRange(CULTIVATION_ID, SENSOR_TYPE_ID);
    }

    @Test
    @DisplayName("이미 통계 row가 있으면 새로 저장하지 않음")
    void doesNotSave_whenStatAlreadyExists() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));
        EnvironmentComplianceStat existingStat = new EnvironmentComplianceStat(CULTIVATION_ID, sensorType);

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(existingStat));

        service.recordCount(event(25.0));

        then(environmentComplianceStatRepository).should(never()).save(any(EnvironmentComplianceStat.class));
        then(environmentComplianceStatRepository).should().incrementInRange(CULTIVATION_ID, SENSOR_TYPE_ID);
    }

    @Test
    @DisplayName("경계값(최댓값과 정확히 같음)은 범위 안으로 판정")
    void treatsBoundaryValueAsInRange() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(new EnvironmentComplianceStat(CULTIVATION_ID, sensorType)));

        service.recordCount(event(28.0));

        then(environmentComplianceStatRepository).should().incrementInRange(CULTIVATION_ID, SENSOR_TYPE_ID);
    }


    @Test
    @DisplayName("타입별 유지 비율을 백분율로 계산해서 반환")
    void returnsComplianceRatePerType() {
        SensorType temperature = new SensorType("TEMPERATURE", "C");
        EnvironmentComplianceStat stat = new EnvironmentComplianceStat(CULTIVATION_ID, temperature);
        ReflectionTestUtils.setField(stat, "inRangeCount", 8);
        ReflectionTestUtils.setField(stat, "outOfRangeCount", 2);

        given(environmentComplianceStatRepository.findAllByCultivationId(CULTIVATION_ID))
                .willReturn(List.of(stat));

        EnvironmentComplianceResponse response = service.getCompliance(CULTIVATION_ID);

        assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(response.humidityCompliance()).isNull();
        assertThat(response.co2Compliance()).isNull();
        assertThat(response.lightCompliance()).isNull();
    }

    @Test
    @DisplayName("아직 데이터가 없는 타입은 null 반환")
    void returnsNull_whenNoDataYet() {
        given(environmentComplianceStatRepository.findAllByCultivationId(CULTIVATION_ID))
                .willReturn(List.of());

        EnvironmentComplianceResponse response = service.getCompliance(CULTIVATION_ID);

        assertThat(response.temperatureCompliance()).isNull();
        assertThat(response.humidityCompliance()).isNull();
        assertThat(response.co2Compliance()).isNull();
        assertThat(response.lightCompliance()).isNull();
    }
}