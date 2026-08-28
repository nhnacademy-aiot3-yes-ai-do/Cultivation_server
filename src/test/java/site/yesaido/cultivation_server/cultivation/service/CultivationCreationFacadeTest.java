package site.yesaido.cultivation_server.cultivation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.cultivation.service.impl.CultivationCreationFacadeImpl;
import site.yesaido.cultivation_server.rabbitmq.event.SensorRange;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.InvalidThresholdRangeException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingService;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationCreationFacadeTest {

    private static final Long USER_ID = 1L;
    private static final Long CULTIVATION_ID = 10L;
    private static final Long MUSHROOM_ID = 100L;

    @Mock
    CultivationService cultivationService;

    @Mock
    SensorTypeService sensorTypeService;

    @Mock
    EnvironmentSettingService environmentSettingService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CultivationCreationFacadeImpl cultivationCreationFacade;

    @Nested
    @DisplayName("경작지 생성")
    class Create {

        @Test
        @DisplayName("경작지와 환경 설정을 저장하고 임계값 이벤트를 발행한다")
        void create_success() {
            // given
            EnvironmentSettingRequest temperatureSetting =
                    new EnvironmentSettingRequest(
                            10L,
                            BigDecimal.valueOf(18),
                            BigDecimal.valueOf(24)
                    );

            EnvironmentSettingRequest humiditySetting =
                    new EnvironmentSettingRequest(
                            20L,
                            BigDecimal.valueOf(60),
                            BigDecimal.valueOf(80)
                    );

            List<EnvironmentSettingRequest> settings =
                    List.of(temperatureSetting, humiditySetting);

            CultivationCreateRequest request =
                    new CultivationCreateRequest(
                            "느타리버섯 1동",
                            MUSHROOM_ID,
                            settings
                    );

            CultivationCreateResponse expectedResponse =
                    new CultivationCreateResponse(
                            CULTIVATION_ID,
                            null,
                            List.of()
                    );

            SensorType temperature =
                    sensorType(10L, "TEMPERATURE", "C");

            SensorType humidity =
                    sensorType(20L, "HUMIDITY", "%");

            List<Long> sensorTypeIds = List.of(10L, 20L);
            List<SensorType> sensorTypes =
                    List.of(temperature, humidity);

            when(sensorTypeService.getSensorTypeList(sensorTypeIds))
                    .thenReturn(sensorTypes);

            when(cultivationService.create(request, USER_ID))
                    .thenReturn(expectedResponse);

            // when
            CultivationCreateResponse actualResponse =
                    cultivationCreationFacade.create(USER_ID, request);

            // then
            assertThat(actualResponse).isEqualTo(expectedResponse);

            ArgumentCaptor<Map<Long, SensorType>> mapCaptor =
                    ArgumentCaptor.forClass(Map.class);

            ArgumentCaptor<ThresholdInfoEvent> eventCaptor =
                    ArgumentCaptor.forClass(ThresholdInfoEvent.class);

            InOrder inOrder = inOrder(
                    sensorTypeService,
                    cultivationService,
                    environmentSettingService,
                    eventPublisher
            );

            inOrder.verify(sensorTypeService)
                    .getSensorTypeList(sensorTypeIds);

            inOrder.verify(cultivationService)
                    .create(request, USER_ID);

            inOrder.verify(environmentSettingService)
                    .apply(
                            eq(CULTIVATION_ID),
                            eq(settings),
                            mapCaptor.capture()
                    );

            inOrder.verify(eventPublisher)
                    .publishEvent(eventCaptor.capture());

            assertThat(mapCaptor.getValue())
                    .containsEntry(10L, temperature)
                    .containsEntry(20L, humidity);

            ThresholdInfoEvent event = eventCaptor.getValue();

            assertThat(event.cultivationId())
                    .isEqualTo(CULTIVATION_ID);

            assertThat(event.occurredAt())
                    .isNotNull();

            assertThat(event.occurredAt().getOffset().getTotalSeconds())
                    .isEqualTo(9 * 60 * 60);

            assertThat(event.sensorRangeList())
                    .extracting(
                            SensorRange::sensorType,
                            SensorRange::unit,
                            SensorRange::minValue,
                            SensorRange::maxValue
                    )
                    .containsExactly(
                            tuple(
                                    "TEMPERATURE",
                                    "C",
                                    BigDecimal.valueOf(18),
                                    BigDecimal.valueOf(24)
                            ),
                            tuple(
                                    "HUMIDITY",
                                    "%",
                                    BigDecimal.valueOf(60),
                                    BigDecimal.valueOf(80)
                            )
                    );
        }

        @Test
        @DisplayName("센서 타입이 존재하지 않으면 경작지를 생성하지 않는다")
        void create_failWhenSensorTypeNotFound() {
            // given
            List<EnvironmentSettingRequest> settings = List.of(
                    new EnvironmentSettingRequest(
                            999L,
                            BigDecimal.ONE,
                            BigDecimal.TEN
                    )
            );

            CultivationCreateRequest request =
                    new CultivationCreateRequest(
                            "테스트 경작지",
                            MUSHROOM_ID,
                            settings
                    );

            when(sensorTypeService.getSensorTypeList(List.of(999L)))
                    .thenThrow(
                            new SensorTypeNotFoundException(
                                    "sensorTypeId:999"
                            )
                    );

            // when & then
            assertThatThrownBy(() ->
                    cultivationCreationFacade.create(USER_ID, request)
            ).isInstanceOf(SensorTypeNotFoundException.class);

            verifyNoInteractions(
                    cultivationService,
                    environmentSettingService,
                    eventPublisher
            );
        }

        @Test
        @DisplayName("환경 설정 저장에 실패하면 임계값 이벤트를 발행하지 않는다")
        void create_failWhenEnvironmentSettingApplyFails() {
            // given
            EnvironmentSettingRequest setting =
                    new EnvironmentSettingRequest(
                            10L,
                            BigDecimal.valueOf(30),
                            BigDecimal.valueOf(20)
                    );

            List<EnvironmentSettingRequest> settings =
                    List.of(setting);

            CultivationCreateRequest request =
                    new CultivationCreateRequest(
                            "잘못된 임계값 경작지",
                            MUSHROOM_ID,
                            settings
                    );

            CultivationCreateResponse response =
                    new CultivationCreateResponse(
                            CULTIVATION_ID,
                            null,
                            List.of()
                    );

            SensorType temperature =
                    sensorType(10L, "TEMPERATURE", "C");

            when(sensorTypeService.getSensorTypeList(List.of(10L)))
                    .thenReturn(List.of(temperature));

            when(cultivationService.create(request, USER_ID))
                    .thenReturn(response);

            doThrow(new InvalidThresholdRangeException("min > max"))
                    .when(environmentSettingService)
                    .apply(
                            eq(CULTIVATION_ID),
                            eq(settings),
                            anyMap()
                    );

            // when & then
            assertThatThrownBy(() ->
                    cultivationCreationFacade.create(USER_ID, request)
            ).isInstanceOf(InvalidThresholdRangeException.class);

            verify(cultivationService)
                    .create(request, USER_ID);

            verify(environmentSettingService)
                    .apply(
                            eq(CULTIVATION_ID),
                            eq(settings),
                            anyMap()
                    );

            verifyNoInteractions(eventPublisher);
        }
    }

    private SensorType sensorType(
            Long id,
            String type,
            String unit
    ) {
        SensorType sensorType = new SensorType(type, unit);
        ReflectionTestUtils.setField(sensorType, "id", id);
        return sensorType;
    }
}