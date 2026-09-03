package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.EnvironmentSettingNotFoundException;
import site.yesaido.cultivation_server.sensor.exception.InvalidThresholdRangeException;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.service.impl.EnvironmentSettingServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentSettingServiceTest {

    @Mock
    EnvironmentSettingRepository environmentSettingRepository;

    @InjectMocks
    EnvironmentSettingServiceImpl service;

    @Test
    @DisplayName("설정이 없으면 새 EnvironmentSetting을 저장")
    void returns_newEnvironmentSetting() {
        //given
        long cultivationId = 1L;
        long sensorTypeId = 10L;

        BigDecimal thresholdMin = new BigDecimal("10.0");
        BigDecimal thresholdMax = new BigDecimal("30.0");

        EnvironmentSettingRequest request = new EnvironmentSettingRequest(sensorTypeId, thresholdMin, thresholdMax);
        List<EnvironmentSettingRequest> requests = List.of(request);

        SensorType mockSensorType = mock(SensorType.class);
        Map<Long, SensorType> sensorTypes = Map.of(sensorTypeId, mockSensorType);

        given(environmentSettingRepository
                .findAllByCultivationIdAndSensorType_IdIn(
                        eq(cultivationId), anyCollection()))
                .willReturn(List.of());

        //when
        service.apply(cultivationId, requests, sensorTypes);


        then(environmentSettingRepository).should(times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("기존 설정이 있으면 threshold를 변경")
    void updatesExistingEnvironmentSetting() {
        long cultivationId = 1L;
        long sensorTypeId = 10L;

        BigDecimal oldMin = new BigDecimal("5.0");
        BigDecimal oldMax = new BigDecimal("20.0");
        BigDecimal newMin = new BigDecimal("10.0");
        BigDecimal newMax = new BigDecimal("30.0");

        SensorType sensorType = mock(SensorType.class);
        given(sensorType.getId()).willReturn(sensorTypeId);

        EnvironmentSetting existingSetting = new EnvironmentSetting(
                cultivationId,
                sensorType,
                oldMin,
                oldMax
        );

        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                sensorTypeId,
                newMin,
                newMax
        );

        given(environmentSettingRepository
                .findAllByCultivationIdAndSensorType_IdIn(
                        eq(cultivationId),
                        anyCollection()
                ))
                .willReturn(List.of(existingSetting));

        service.apply(cultivationId, List.of(request), Map.of(sensorTypeId, sensorType));

        assertThat(existingSetting.getThresholdMin())
                .isEqualByComparingTo(newMin);
        assertThat(existingSetting.getThresholdMax())
                .isEqualByComparingTo(newMax);

        then(environmentSettingRepository).should()
                .findAllByCultivationIdAndSensorType_IdIn(
                        eq(cultivationId),
                        argThat(ids ->
                                ids.size() == 1
                                        && ids.contains(sensorTypeId))
                );

        then(environmentSettingRepository)
                .should(never())
                .saveAll(anyList());
    }

    @Test
    @DisplayName("thresholdMin > thresholdMax이면 예외 발생")
    void testThresholdException() {
        long cultivationId = 1L;
        long sensorTypeId = 10L;

        BigDecimal thresholdMin = new BigDecimal("30.0");
        BigDecimal thresholdMax = new BigDecimal("10.0");

        EnvironmentSettingRequest request = new EnvironmentSettingRequest(sensorTypeId, thresholdMin, thresholdMax);
        List<EnvironmentSettingRequest> requests = List.of(request);

        SensorType mockSensorType = mock(SensorType.class);
        Map<Long, SensorType> sensorTypes = Map.of(sensorTypeId, mockSensorType);

        assertThatThrownBy(() -> service.apply(cultivationId, requests, sensorTypes))
                .isInstanceOf(InvalidThresholdRangeException.class);
    }

    @Test
    @DisplayName("기존 환경설정의 임계값만 수정")
    void updateExistingChangesThresholds() {
        long cultivationId = 1L;
        long sensorTypeId = 10L;
        SensorType sensorType = mock(SensorType.class);
        EnvironmentSetting setting = new EnvironmentSetting(
                cultivationId,
                sensorType,
                new BigDecimal("18.0"),
                new BigDecimal("24.0")
        );
        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                sensorTypeId,
                new BigDecimal("19.0"),
                new BigDecimal("25.0")
        );

        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(cultivationId, sensorTypeId))
                .willReturn(Optional.of(setting));

        service.updateExisting(cultivationId, request);

        assertThat(setting.getThresholdMin()).isEqualByComparingTo("19.0");
        assertThat(setting.getThresholdMax()).isEqualByComparingTo("25.0");
        then(environmentSettingRepository).should(never()).save(any(EnvironmentSetting.class));
        then(environmentSettingRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("기존 환경설정이 없으면 새로 만들지 않고 404 예외")
    void updateExistingRejectsMissingSetting() {
        long cultivationId = 1L;
        long sensorTypeId = 10L;
        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                sensorTypeId,
                new BigDecimal("19.0"),
                new BigDecimal("25.0")
        );

        given(environmentSettingRepository.findByCultivationIdAndSensorType_Id(cultivationId, sensorTypeId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateExisting(cultivationId, request))
                .isInstanceOf(EnvironmentSettingNotFoundException.class);

        then(environmentSettingRepository).should(never()).save(any(EnvironmentSetting.class));
        then(environmentSettingRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("기존 환경설정 수정에서도 최소값이 최대값보다 크면 예외")
    void updateExistingRejectsInvalidRangeBeforeLookup() {
        EnvironmentSettingRequest request = new EnvironmentSettingRequest(
                10L,
                new BigDecimal("30.0"),
                new BigDecimal("10.0")
        );

        assertThatThrownBy(() -> service.updateExisting(1L, request))
                .isInstanceOf(InvalidThresholdRangeException.class);

        then(environmentSettingRepository).shouldHaveNoInteractions();
    }

}
