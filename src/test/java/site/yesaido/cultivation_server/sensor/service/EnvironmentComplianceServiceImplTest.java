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
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.repository.InfluxSensorQueryRepository;
import site.yesaido.cultivation_server.sensor.service.impl.EnvironmentComplianceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EnvironmentComplianceServiceImplTest {

    private static final Long CULTIVATION_ID = 1L;
    private static final Long TEMPERATURE_TYPE_ID = 10L;
    private static final Long HUMIDITY_TYPE_ID = 11L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 8, 7);

    @Mock
    EnvironmentSettingRepository environmentSettingRepository;

    @Mock
    InfluxSensorQueryRepository influxSensorQueryRepository;

    @Mock
    CultivationRepository cultivationRepository;

    @InjectMocks
    EnvironmentComplianceServiceImpl service;

    private SensorType temperatureType() {
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        ReflectionTestUtils.setField(sensorType, "id", TEMPERATURE_TYPE_ID);
        return sensorType;
    }

    private SensorType humidityType() {
        SensorType sensorType = new SensorType("HUMIDITY", "%");
        ReflectionTestUtils.setField(sensorType, "id", HUMIDITY_TYPE_ID);
        return sensorType;
    }

    private EnvironmentSetting settingFor(SensorType sensorType, BigDecimal min, BigDecimal max) {
        return new EnvironmentSetting(CULTIVATION_ID, sensorType, min, max);
    }

    @Nested
    @DisplayName("getComplianceForPeriod")
    class GetComplianceForPeriod {

        @Test
        @DisplayName("등록된 센서타입이 없으면 전부 null 반환")
        void returnsAllNull_whenNoSettings() {
            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(existingCultivation()));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of());

            EnvironmentComplianceResponse response = service.getComplianceForPeriod(CULTIVATION_ID, START_DATE, END_DATE);

            assertThat(response.temperatureCompliance()).isNull();
            assertThat(response.humidityCompliance()).isNull();
            assertThat(response.co2Compliance()).isNull();
            assertThat(response.lightCompliance()).isNull();
            then(influxSensorQueryRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("InfluxDB에 아직 데이터가 없는(total=0) 타입은 null 반환하고 countInRange는 호출 안 함")
        void returnsNull_whenNoInfluxDataYet() {
            SensorType temperature = temperatureType();
            EnvironmentSetting setting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(existingCultivation()));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of(setting));
            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", START_DATE, END_DATE))
                    .willReturn(0L);

            EnvironmentComplianceResponse response = service.getComplianceForPeriod(CULTIVATION_ID, START_DATE, END_DATE);

            assertThat(response.temperatureCompliance()).isNull();
            then(influxSensorQueryRepository).should(never())
                    .countInRange(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("in-range/total 비율을 퍼센트로 계산")
        void computesRate() {
            SensorType temperature = temperatureType();
            EnvironmentSetting setting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(existingCultivation()));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of(setting));
            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", START_DATE, END_DATE))
                    .willReturn(10L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "TEMPERATURE", START_DATE, END_DATE,
                    BigDecimal.valueOf(18), BigDecimal.valueOf(28)))
                    .willReturn(8L);

            EnvironmentComplianceResponse response = service.getComplianceForPeriod(CULTIVATION_ID, START_DATE, END_DATE);

            assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(response.humidityCompliance()).isNull();
        }

        @Test
        @DisplayName("여러 센서타입은 각각 독립적으로 계산")
        void computesEachSensorTypeIndependently() {
            SensorType temperature = temperatureType();
            SensorType humidity = humidityType();
            EnvironmentSetting tempSetting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));
            EnvironmentSetting humiditySetting = settingFor(humidity, BigDecimal.valueOf(50), BigDecimal.valueOf(70));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(existingCultivation()));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID))
                    .willReturn(List.of(tempSetting, humiditySetting));

            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", START_DATE, END_DATE))
                    .willReturn(10L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "TEMPERATURE", START_DATE, END_DATE,
                    BigDecimal.valueOf(18), BigDecimal.valueOf(28)))
                    .willReturn(10L);

            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "HUMIDITY", START_DATE, END_DATE))
                    .willReturn(4L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "HUMIDITY", START_DATE, END_DATE,
                    BigDecimal.valueOf(50), BigDecimal.valueOf(70)))
                    .willReturn(1L);

            EnvironmentComplianceResponse response = service.getComplianceForPeriod(CULTIVATION_ID, START_DATE, END_DATE);

            assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(response.humidityCompliance()).isEqualByComparingTo(BigDecimal.valueOf(25));
        }
    }

    @Nested
    @DisplayName("getDailyCompliance")
    class GetDailyCompliance {

        @Test
        @DisplayName("시작일=종료일=해당 날짜로 getComplianceForPeriod에 위임")
        void delegatesWithSameStartAndEndDate() {
            SensorType temperature = temperatureType();
            EnvironmentSetting setting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(existingCultivation()));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of(setting));
            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", END_DATE, END_DATE))
                    .willReturn(2L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "TEMPERATURE", END_DATE, END_DATE,
                    BigDecimal.valueOf(18), BigDecimal.valueOf(28)))
                    .willReturn(1L);

            EnvironmentComplianceResponse response = service.getDailyCompliance(CULTIVATION_ID, END_DATE);

            assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        }
    }

    @Nested
    @DisplayName("getCompliance (누적)")
    class GetCompliance {

        @Test
        @DisplayName("존재하지 않는 재배면 예외 발생")
        void throws_whenCultivationNotFound() {
            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCompliance(CULTIVATION_ID))
                    .isInstanceOf(CultivationNotFoundException.class);

            then(environmentSettingRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("startedAt이 있으면 startedAt을 시작일로 사용해서 InfluxDB 조회")
        void usesStartedAt_whenPresent() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 7, 20, 9, 0);
            Cultivation cultivation = Cultivation.builder()
                    .id(CULTIVATION_ID)
                    .startedAt(startedAt)
                    .createdAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                    .build();

            SensorType temperature = temperatureType();
            EnvironmentSetting setting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(cultivation));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of(setting));

            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", startedAt.toLocalDate(), today))
                    .willReturn(4L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "TEMPERATURE", startedAt.toLocalDate(), today,
                    BigDecimal.valueOf(18), BigDecimal.valueOf(28)))
                    .willReturn(4L);

            EnvironmentComplianceResponse response = service.getCompliance(CULTIVATION_ID);

            assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("startedAt이 없으면 createdAt을 시작일로 사용해서 InfluxDB 조회")
        void usesCreatedAt_whenStartedAtIsNull() {
            LocalDateTime createdAt = LocalDateTime.of(2026, 7, 15, 0, 0);
            Cultivation cultivation = Cultivation.builder()
                    .id(CULTIVATION_ID)
                    .startedAt(null)
                    .createdAt(createdAt)
                    .build();

            SensorType temperature = temperatureType();
            EnvironmentSetting setting = settingFor(temperature, BigDecimal.valueOf(18), BigDecimal.valueOf(28));

            given(cultivationRepository.findById(CULTIVATION_ID)).willReturn(Optional.of(cultivation));
            given(environmentSettingRepository.findAllByCultivationId(CULTIVATION_ID)).willReturn(List.of(setting));

            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            given(influxSensorQueryRepository.countTotal(CULTIVATION_ID, "TEMPERATURE", createdAt.toLocalDate(), today))
                    .willReturn(5L);
            given(influxSensorQueryRepository.countInRange(
                    CULTIVATION_ID, "TEMPERATURE", createdAt.toLocalDate(), today,
                    BigDecimal.valueOf(18), BigDecimal.valueOf(28)))
                    .willReturn(5L);

            EnvironmentComplianceResponse response = service.getCompliance(CULTIVATION_ID);

            assertThat(response.temperatureCompliance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    // Helper Method
    private Cultivation existingCultivation() {
        return Cultivation.builder().id(CULTIVATION_ID).build();
    }
}