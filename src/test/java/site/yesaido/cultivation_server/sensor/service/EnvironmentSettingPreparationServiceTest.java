package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.DuplicateSensorTypeException;
import site.yesaido.cultivation_server.sensor.exception.InvalidThresholdRangeException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeUnitConflictException;
import site.yesaido.cultivation_server.sensor.service.impl.EnvironmentSettingPreparationServiceImpl;
import site.yesaido.cultivation_server.sensor.service.model.PreparedEnvironmentSettings;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static site.yesaido.cultivation_server.sensor.support.SensorUnits.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentSettingPreparationServiceTest {

    @Mock
    SensorTypeService sensorTypeService;

    EnvironmentSettingPreparationService preparationService;

    @BeforeEach
    void setUp() {
        preparationService = new EnvironmentSettingPreparationServiceImpl(
                sensorTypeService,
                new TemperatureThresholdConverter()
        );
    }

    @Test
    @DisplayName("섭씨 임계값 요청을 섭씨와 화씨 설정으로 확장한다")
    void prepare_expandsCelsiusToTemperaturePair() {
        SensorType celsius = sensorType(10L, TEMPERATURE, CELSIUS);
        SensorType fahrenheit = sensorType(11L, TEMPERATURE, FAHRENHEIT);
        EnvironmentSettingRequest request = setting(10L, "18", "24");

        when(sensorTypeService.getSensorTypeList(List.of(10L)))
                .thenReturn(List.of(celsius));
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, CELSIUS))
                .thenReturn(celsius);
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, FAHRENHEIT))
                .thenReturn(fahrenheit);

        PreparedEnvironmentSettings result =
                preparationService.prepare(List.of(request));

        assertThat(result.requests()).containsExactly(
                setting(10L, "18.0000", "24.0000"),
                setting(11L, "64.4000", "75.2000")
        );
        assertThat(result.sensorTypeMap())
                .containsEntry(10L, celsius)
                .containsEntry(11L, fahrenheit);
    }

    @Test
    @DisplayName("화씨 임계값 요청도 섭씨 기준으로 환산해 두 단위를 만든다")
    void prepare_expandsFahrenheitThroughCelsius() {
        SensorType celsius = sensorType(10L, TEMPERATURE, CELSIUS);
        SensorType fahrenheit = sensorType(11L, TEMPERATURE, FAHRENHEIT);
        EnvironmentSettingRequest request = setting(11L, "64.4", "75.2");

        when(sensorTypeService.getSensorTypeList(List.of(11L)))
                .thenReturn(List.of(fahrenheit));
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, CELSIUS))
                .thenReturn(celsius);
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, FAHRENHEIT))
                .thenReturn(fahrenheit);

        PreparedEnvironmentSettings result =
                preparationService.prepare(List.of(request));

        assertThat(result.requests()).containsExactly(
                setting(10L, "18.0000", "24.0000"),
                setting(11L, "64.4000", "75.2000")
        );
    }

    @Test
    @DisplayName("온도가 아닌 환경 설정은 변경하지 않는다")
    void prepare_keepsNonTemperatureSetting() {
        SensorType humidity = sensorType(20L, "HUMIDITY", "%");
        EnvironmentSettingRequest request = setting(20L, "60", "80");

        when(sensorTypeService.getSensorTypeList(List.of(20L)))
                .thenReturn(List.of(humidity));

        PreparedEnvironmentSettings result =
                preparationService.prepare(List.of(request));

        assertThat(result.requests()).containsExactly(request);
        assertThat(result.sensorTypeMap()).containsOnlyKeys(20L);
        verify(sensorTypeService, never())
                .getByTypeAndValueUnit(anyString(), anyString());
    }

    @Test
    @DisplayName("같은 센서 타입 ID가 중복되면 조회 전에 거부한다")
    void prepare_rejectsDuplicateSensorTypeId() {
        List<EnvironmentSettingRequest> requests = List.of(
                setting(10L, "18", "24"),
                setting(10L, "19", "23")
        );

        assertThatThrownBy(() -> preparationService.prepare(requests))
                .isInstanceOf(DuplicateSensorTypeException.class);

        verifyNoInteractions(sensorTypeService);
    }

    @Test
    @DisplayName("섭씨와 화씨를 동시에 요청하면 충돌로 거부한다")
    void prepare_rejectsTwoTemperatureUnits() {
        SensorType celsius = sensorType(10L, TEMPERATURE, CELSIUS);
        SensorType fahrenheit = sensorType(11L, TEMPERATURE, FAHRENHEIT);
        List<EnvironmentSettingRequest> requests = List.of(
                setting(10L, "18", "24"),
                setting(11L, "64.4", "75.2")
        );

        when(sensorTypeService.getSensorTypeList(List.of(10L, 11L)))
                .thenReturn(List.of(celsius, fahrenheit));

        assertThatThrownBy(() -> preparationService.prepare(requests))
                .isInstanceOf(SensorTypeUnitConflictException.class);

        verify(sensorTypeService, never())
                .getByTypeAndValueUnit(anyString(), anyString());
    }

    @Test
    @DisplayName("온도 최솟값이 최댓값보다 크면 거부한다")
    void prepare_rejectsInvalidTemperatureRange() {
        SensorType celsius = sensorType(10L, TEMPERATURE, CELSIUS);
        SensorType fahrenheit = sensorType(11L, TEMPERATURE, FAHRENHEIT);
        EnvironmentSettingRequest request = setting(10L, "30", "20");

        when(sensorTypeService.getSensorTypeList(List.of(10L)))
                .thenReturn(List.of(celsius));
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, CELSIUS))
                .thenReturn(celsius);
        when(sensorTypeService.getByTypeAndValueUnit(TEMPERATURE, FAHRENHEIT))
                .thenReturn(fahrenheit);

        List<EnvironmentSettingRequest> requests = List.of(request);

        assertThatThrownBy(() -> preparationService.prepare(requests))
                .isInstanceOf(InvalidThresholdRangeException.class);
    }

    private EnvironmentSettingRequest setting(
            long sensorTypeId,
            String min,
            String max
    ) {
        return new EnvironmentSettingRequest(
                sensorTypeId,
                new BigDecimal(min),
                new BigDecimal(max)
        );
    }

    private SensorType sensorType(
            long id,
            String type,
            String unit
    ) {
        SensorType sensorType = new SensorType(type, unit);
        ReflectionTestUtils.setField(sensorType, "id", id);
        return sensorType;
    }
}
