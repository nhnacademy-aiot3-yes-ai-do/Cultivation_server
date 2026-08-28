package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationModeChangeResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationMode;
import site.yesaido.cultivation_server.cultivation.exception.CultivationAlreadyInHarvestModeException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.rabbitmq.event.SensorRange;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThresholdType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorTypeRepository;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceThresholdRepository;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationModeFacadeImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationModeFacadeTest {

    private static final Long USER_ID = 1L;
    private static final Long CULTIVATION_ID = 10L;
    private static final Long MUSHROOM_ID = 100L;

    @Mock
    CultivationService cultivationService;

    @Mock
    CultivationRepository cultivationRepository;

    @Mock
    MushroomReferenceThresholdRepository mushroomReferenceThresholdRepository;

    @Mock
    CultivationSensorTypeRepository cultivationSensorTypeRepository;

    @Mock
    EnvironmentSettingService environmentSettingService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CultivationModeFacadeImpl cultivationModeFacade;

    @Nested
    @DisplayName("수확 모드 전환")
    class SwitchToHarvestMode {

        @Test
        @DisplayName("등록된 센서 타입에 해당하는 수확 임계값을 적용하고 이벤트를 발행한다")
        void switchToHarvestMode_success() {
            CultivationModeChangeResponse expectedResponse =
                    new CultivationModeChangeResponse(CULTIVATION_ID, CultivationMode.HARVEST);
            when(cultivationService.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .thenReturn(expectedResponse);

            Cultivation cultivation = Cultivation.builder()
                    .name("느타리버섯 1동")
                    .mushroomReference(mushroomReference(MUSHROOM_ID))
                    .build();
            when(cultivationRepository.findById(CULTIVATION_ID)).thenReturn(Optional.of(cultivation));

            SensorType temperature = sensorType(10L, "TEMPERATURE", "C");
            SensorType humidity = sensorType(20L, "HUMIDITY", "%");
            when(cultivationSensorTypeRepository.findDistinctSensorTypesByCultivationId(CULTIVATION_ID))
                    .thenReturn(List.of(temperature, humidity));

            when(mushroomReferenceThresholdRepository
                    .findAllByMushroomReference_idAndThresholdType(MUSHROOM_ID, MushroomReferenceThresholdType.HARVEST))
                    .thenReturn(List.of(
                            threshold(temperature, BigDecimal.valueOf(20), BigDecimal.valueOf(28)),
                            threshold(humidity, BigDecimal.valueOf(70), BigDecimal.valueOf(90))
                    ));

            CultivationModeChangeResponse actual =
                    cultivationModeFacade.switchToHarvestMode(CULTIVATION_ID, USER_ID);

            assertThat(actual).isEqualTo(expectedResponse);

            ArgumentCaptor<List<EnvironmentSettingRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<ThresholdInfoEvent> eventCaptor = ArgumentCaptor.forClass(ThresholdInfoEvent.class);

            verify(environmentSettingService).apply(eq(CULTIVATION_ID), requestsCaptor.capture(), anyMap());
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(requestsCaptor.getValue())
                    .extracting(EnvironmentSettingRequest::sensorTypeId, EnvironmentSettingRequest::thresholdMin, EnvironmentSettingRequest::thresholdMax)
                    .containsExactly(
                            tuple(10L, BigDecimal.valueOf(20), BigDecimal.valueOf(28)),
                            tuple(20L, BigDecimal.valueOf(70), BigDecimal.valueOf(90))
                    );

            ThresholdInfoEvent event = eventCaptor.getValue();
            assertThat(event.cultivationId()).isEqualTo(CULTIVATION_ID);
            assertThat(event.sensorRangeList())
                    .extracting(SensorRange::sensorType, SensorRange::unit, SensorRange::minValue, SensorRange::maxValue)
                    .containsExactly(
                            tuple("TEMPERATURE", "C", BigDecimal.valueOf(20), BigDecimal.valueOf(28)),
                            tuple("HUMIDITY", "%", BigDecimal.valueOf(70), BigDecimal.valueOf(90))
                    );
        }

        @Test
        @DisplayName("등록된 물리 센서가 없으면 임계값 조회는 생략하지만 빈 임계값 이벤트는 발행한다")
        void switchToHarvestMode_noRegisteredSensors_publishesEmptyEvent() {
            when(cultivationService.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .thenReturn(new CultivationModeChangeResponse(CULTIVATION_ID, CultivationMode.HARVEST));

            when(cultivationRepository.findById(CULTIVATION_ID)).thenReturn(Optional.of(
                    Cultivation.builder().name("느타리버섯 1동").mushroomReference(mushroomReference(MUSHROOM_ID)).build()
            ));
            when(cultivationSensorTypeRepository.findDistinctSensorTypesByCultivationId(CULTIVATION_ID))
                    .thenReturn(List.of());

            cultivationModeFacade.switchToHarvestMode(CULTIVATION_ID, USER_ID);

            verifyNoInteractions(mushroomReferenceThresholdRepository, environmentSettingService);

            ArgumentCaptor<ThresholdInfoEvent> eventCaptor = ArgumentCaptor.forClass(ThresholdInfoEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().cultivationId()).isEqualTo(CULTIVATION_ID);
            assertThat(eventCaptor.getValue().sensorRangeList()).isEmpty();
        }

        @Test
        @DisplayName("등록된 센서에 해당하는 수확 임계값 참고자료가 없으면 환경설정 적용은 생략하지만 빈 임계값 이벤트는 발행한다")
        void switchToHarvestMode_noMatchingHarvestThreshold_publishesEmptyEvent() {
            when(cultivationService.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .thenReturn(new CultivationModeChangeResponse(CULTIVATION_ID, CultivationMode.HARVEST));

            when(cultivationRepository.findById(CULTIVATION_ID)).thenReturn(Optional.of(
                    Cultivation.builder().name("느타리버섯 1동").mushroomReference(mushroomReference(MUSHROOM_ID)).build()
            ));

            SensorType co2 = sensorType(30L, "CO2", "ppm");
            when(cultivationSensorTypeRepository.findDistinctSensorTypesByCultivationId(CULTIVATION_ID))
                    .thenReturn(List.of(co2));
            when(mushroomReferenceThresholdRepository
                    .findAllByMushroomReference_idAndThresholdType(MUSHROOM_ID, MushroomReferenceThresholdType.HARVEST))
                    .thenReturn(List.of());

            cultivationModeFacade.switchToHarvestMode(CULTIVATION_ID, USER_ID);

            verifyNoInteractions(environmentSettingService);

            ArgumentCaptor<ThresholdInfoEvent> eventCaptor = ArgumentCaptor.forClass(ThresholdInfoEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().sensorRangeList()).isEmpty();
        }

        @Test
        @DisplayName("재배지에 등록되지 않은 센서 타입의 수확 임계값은 적용하지 않는다")
        void switchToHarvestMode_filtersThresholdsByRegisteredSensorTypes() {
            when(cultivationService.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .thenReturn(new CultivationModeChangeResponse(CULTIVATION_ID, CultivationMode.HARVEST));

            when(cultivationRepository.findById(CULTIVATION_ID)).thenReturn(Optional.of(
                    Cultivation.builder().name("느타리버섯 1동").mushroomReference(mushroomReference(MUSHROOM_ID)).build()
            ));

            SensorType temperature = sensorType(10L, "TEMPERATURE", "C");
            when(cultivationSensorTypeRepository.findDistinctSensorTypesByCultivationId(CULTIVATION_ID))
                    .thenReturn(List.of(temperature));

            SensorType co2 = sensorType(30L, "CO2", "ppm"); // 재배지에 미등록
            when(mushroomReferenceThresholdRepository
                    .findAllByMushroomReference_idAndThresholdType(MUSHROOM_ID, MushroomReferenceThresholdType.HARVEST))
                    .thenReturn(List.of(
                            threshold(temperature, BigDecimal.valueOf(20), BigDecimal.valueOf(28)),
                            threshold(co2, BigDecimal.valueOf(400), BigDecimal.valueOf(1000))
                    ));

            cultivationModeFacade.switchToHarvestMode(CULTIVATION_ID, USER_ID);

            ArgumentCaptor<List<EnvironmentSettingRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);
            verify(environmentSettingService).apply(eq(CULTIVATION_ID), requestsCaptor.capture(), anyMap());

            assertThat(requestsCaptor.getValue())
                    .extracting(EnvironmentSettingRequest::sensorTypeId)
                    .containsExactly(10L);
        }

        @Test
        @DisplayName("모드 전환 자체가 실패하면 임계값 동기화를 시도하지 않는다")
        void switchToHarvestMode_failsBeforeSync_whenModeChangeFails() {
            when(cultivationService.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .thenThrow(new CultivationAlreadyInHarvestModeException(CULTIVATION_ID));

            assertThatThrownBy(() -> cultivationModeFacade.switchToHarvestMode(CULTIVATION_ID, USER_ID))
                    .isInstanceOf(CultivationAlreadyInHarvestModeException.class);

            verifyNoInteractions(cultivationRepository, mushroomReferenceThresholdRepository,
                    cultivationSensorTypeRepository, environmentSettingService, eventPublisher);
        }
    }

    private MushroomReference mushroomReference(Long id) {
        MushroomReference mushroomReference = new MushroomReference("느타리버섯", "Oyster Mushroom", "Pleurotus ostreatus");
        ReflectionTestUtils.setField(mushroomReference, "id", id);
        return mushroomReference;
    }

    private SensorType sensorType(Long id, String type, String unit) {
        SensorType sensorType = new SensorType(type, unit);
        ReflectionTestUtils.setField(sensorType, "id", id);
        return sensorType;
    }

    private MushroomReferenceThreshold threshold(SensorType sensorType, BigDecimal min, BigDecimal max) {
        return new MushroomReferenceThreshold(sensorType, null, MushroomReferenceThresholdType.HARVEST, min, max);
    }
}