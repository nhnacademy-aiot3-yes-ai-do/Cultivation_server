package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EnvironmentComplianceServiceImplTest {

    private static final long CULTIVATION_ID = 10L;
    private static final long SENSOR_TYPE_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

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
    @DisplayName("값이 임계값 범위 안이면 오늘 날짜로 incrementInRange 호출")
    void incrementsInRange_whenValueWithinThreshold() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository
                .findByCultivationIdAndSensorType_IdAndStatDate(CULTIVATION_ID, SENSOR_TYPE_ID, TODAY))
                .willReturn(Optional.empty());

        service.recordCount(event(25.0));

        then(environmentComplianceStatRepository).should().save(any(EnvironmentComplianceStat.class));
        then(environmentComplianceStatRepository).should()
                .incrementInRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
        then(environmentComplianceStatRepository).should(never())
                .incrementOutOfRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
    }

    @Test
    @DisplayName("값이 임계값 범위 밖이면 오늘 날짜로 incrementOutOfRange 호출")
    void incrementsOutOfRange_whenValueOutsideThreshold() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository
                .findByCultivationIdAndSensorType_IdAndStatDate(CULTIVATION_ID, SENSOR_TYPE_ID, TODAY))
                .willReturn(Optional.empty());

        service.recordCount(event(35.0));

        then(environmentComplianceStatRepository).should()
                .incrementOutOfRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
        then(environmentComplianceStatRepository).should(never())
                .incrementInRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
    }

    @Test
    @DisplayName("오늘치 row가 이미 있으면 새로 저장하지 않음")
    void doesNotSave_whenTodayStatAlreadyExists() {
        SensorType sensorType = temperatureType();
        EnvironmentSetting setting = new EnvironmentSetting(
                CULTIVATION_ID, sensorType, BigDecimal.valueOf(18), BigDecimal.valueOf(28));
        EnvironmentComplianceStat existingStat =
                new EnvironmentComplianceStat(CULTIVATION_ID, sensorType, TODAY);

        given(sensorTypeRepository.findByType("TEMPERATURE")).willReturn(Optional.of(sensorType));
        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(CULTIVATION_ID, SENSOR_TYPE_ID))
                .willReturn(Optional.of(setting));
        given(environmentComplianceStatRepository
                .findByCultivationIdAndSensorType_IdAndStatDate(CULTIVATION_ID, SENSOR_TYPE_ID, TODAY))
                .willReturn(Optional.of(existingStat));

        service.recordCount(event(25.0));

        then(environmentComplianceStatRepository).should(never()).save(any(EnvironmentComplianceStat.class));
        then(environmentComplianceStatRepository).should()
                .incrementInRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
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
        given(environmentComplianceStatRepository
                .findByCultivationIdAndSensorType_IdAndStatDate(CULTIVATION_ID, SENSOR_TYPE_ID, TODAY))
                .willReturn(Optional.of(new EnvironmentComplianceStat(CULTIVATION_ID, sensorType, TODAY)));

        service.recordCount(event(28.0));

        then(environmentComplianceStatRepository).should()
                .incrementInRange(eq(CULTIVATION_ID), eq(SENSOR_TYPE_ID), eq(TODAY));
    }

    @Test
    @DisplayName("여러 날짜에 걸친 row를 전부 합산해서 비율로 반환")
    void sumsAcrossAllDates() {
        SensorType temperature = new SensorType("TEMPERATURE", "C");

        EnvironmentComplianceStat day1 = new EnvironmentComplianceStat(CULTIVATION_ID, temperature, TODAY.minusDays(1));
        ReflectionTestUtils.setField(day1, "inRangeCount", 5);
        ReflectionTestUtils.setField(day1, "outOfRangeCount", 5);

        EnvironmentComplianceStat day2 = new EnvironmentComplianceStat(CULTIVATION_ID, temperature, TODAY);
        ReflectionTestUtils.setField(day2, "inRangeCount", 3);
        ReflectionTestUtils.setField(day2, "outOfRangeCount", 7);

        given(environmentComplianceStatRepository.findAllByCultivationId(CULTIVATION_ID))
                .willReturn(List.of(day1, day2));

        EnvironmentComplianceResponse response = service.getCompliance(CULTIVATION_ID);

        // (5+3) / (10+10) * 100 = 40.00
        assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.humidityCompliance()).isNull();
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

    @Test
    @DisplayName("해당 날짜의 row만 조회해서 비율로 반환")
    void returnsRateForGivenDateOnly() {
        SensorType temperature = new SensorType("TEMPERATURE", "C");
        EnvironmentComplianceStat stat = new EnvironmentComplianceStat(CULTIVATION_ID, temperature, TODAY);
        ReflectionTestUtils.setField(stat, "inRangeCount", 8);
        ReflectionTestUtils.setField(stat, "outOfRangeCount", 2);

        given(environmentComplianceStatRepository.findAllByCultivationIdAndStatDate(CULTIVATION_ID, TODAY))
                .willReturn(List.of(stat));

        EnvironmentComplianceResponse response = service.getDailyCompliance(CULTIVATION_ID, TODAY);

        assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    @DisplayName("해당 날짜에 데이터가 없으면 null 반환")
    void returnsNull_whenNoDataForDate() {
        given(environmentComplianceStatRepository.findAllByCultivationIdAndStatDate(CULTIVATION_ID, TODAY))
                .willReturn(List.of());

        EnvironmentComplianceResponse response = service.getDailyCompliance(CULTIVATION_ID, TODAY);

        assertThat(response.temperatureCompliance()).isNull();
    }

}