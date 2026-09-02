package site.yesaido.cultivation_server.sensor.service;

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
import site.yesaido.cultivation_server.cultivation.exception.CultivationAccessDeniedException;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoDeleteEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorRange;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorTypeResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorFacadeImpl;
import site.yesaido.cultivation_server.sensor.service.model.PreparedEnvironmentSettings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CultivationSensorFacadeTest {

    private static final long USER_ID = 1L;
    private static final long CULTIVATION_ID = 10L;
    private static final long SENSOR_ID = 100L;

    @Mock
    CultivationMemberService cultivationMemberService;

    @Mock
    SensorTypeService sensorTypeService;

    @Mock
    CultivationSensorService cultivationSensorService;

    @Mock
    CultivationSensorTypeService cultivationSensorTypeService;

    @Mock
    EnvironmentSettingService environmentSettingService;

    @Mock
    EnvironmentSettingPreparationService environmentSettingPreparationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CultivationSensorFacadeImpl cultivationSensorFacade;

    /**
     * 1. 등록 성공
     * → 타입 조회
     * → 센서 등록
     * → 타입 연결
     * → 환경설정 적용
     * → 센서 ID 반환
     * <p>
     * 2. 삭제 성공
     * → 접근 권한 확인
     * → sensorService.delete() 호출
     */

    @Nested
    @DisplayName("등록")
    class Facade_register {

        @Test
        @DisplayName("등록 성공")
            // 센서 타입 조회부터 환경설정 적용까지 순서대로 처리
        void register_success() {
            // given
            EnvironmentSettingRequest temperatureSetting =
                    new EnvironmentSettingRequest(
                            10L,
                            BigDecimal.valueOf(20),
                            BigDecimal.valueOf(30)
                    );

            EnvironmentSettingRequest humiditySetting =
                    new EnvironmentSettingRequest(
                            20L,
                            BigDecimal.valueOf(40),
                            BigDecimal.valueOf(70)
                    );

            EnvironmentSettingRequest fahrenheitSetting =
                    new EnvironmentSettingRequest(
                            11L,
                            new BigDecimal("68.0000"),
                            new BigDecimal("86.0000")
                    );

            List<EnvironmentSettingRequest> settings =
                    List.of(temperatureSetting, humiditySetting);

            List<EnvironmentSettingRequest> preparedSettings =
                    List.of(
                            temperatureSetting,
                            fahrenheitSetting,
                            humiditySetting
                    );

            CreateCultivationSensorRequest request =
                    new CreateCultivationSensorRequest(
                            "EUI-001",
                            "MODEL-A",
                            "배양실 센서",
                            "ROOM-1",
                            "북쪽 선반",
                            settings
                    );

            SensorType temperature =
                    new SensorType("TEMPERATURE", "°C");
            SensorType fahrenheit =
                    new SensorType("TEMPERATURE", "°F");
            SensorType humidity =
                    new SensorType("HUMIDITY", "%");

            ReflectionTestUtils.setField(
                    temperature,
                    "id",
                    temperatureSetting.sensorTypeId()
            );
            ReflectionTestUtils.setField(
                    fahrenheit,
                    "id",
                    fahrenheitSetting.sensorTypeId()
            );
            ReflectionTestUtils.setField(
                    humidity,
                    "id",
                    humiditySetting.sensorTypeId()
            );

            List<SensorType> sensorTypes =
                    List.of(temperature, fahrenheit, humidity);

            Map<Long, SensorType> sensorTypeMap = Map.of(
                    10L, temperature,
                    11L, fahrenheit,
                    20L, humidity
            );

            CultivationSensor sensor = new CultivationSensor(
                    CULTIVATION_ID,
                    request.deviceEui(),
                    request.deviceModel(),
                    request.deviceName(),
                    request.location(),
                    request.locationDetail()
            );

            ReflectionTestUtils.setField(
                    sensor,
                    "id",
                    SENSOR_ID
            );

            when(environmentSettingPreparationService.prepare(settings))
                    .thenReturn(new PreparedEnvironmentSettings(
                            preparedSettings,
                            sensorTypeMap
                    ));

            when(cultivationSensorService.register(CULTIVATION_ID, request))
                    .thenReturn(sensor);


            // when
            long registeredSensorId = cultivationSensorFacade.register(USER_ID, CULTIVATION_ID, request);

            // then
            assertThat(registeredSensorId).isEqualTo(SENSOR_ID);

            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);

            verify(eventPublisher, times(4))
                    .publishEvent(eventCaptor.capture());

            List<Object> events = eventCaptor.getAllValues();

            List<SensorInfoUpsertEvent> upserts = events.stream()
                            .filter(SensorInfoUpsertEvent.class::isInstance)
                            .map(SensorInfoUpsertEvent.class::cast)
                            .toList();

            ThresholdInfoEvent threshold = events.stream()
                    .filter(ThresholdInfoEvent.class::isInstance)
                    .map(ThresholdInfoEvent.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertThat(upserts).hasSize(3);
            assertThat(threshold.cultivationId()).isEqualTo(CULTIVATION_ID);

            assertThat(threshold.sensorRangeList())
                    .extracting(
                            SensorRange::sensorType,
                            SensorRange::unit,
                            SensorRange::minValue,
                            SensorRange::maxValue
                    )
                            .containsExactly(
                                    tuple(
                                            "TEMPERATURE",
                                            "°C",
                                            BigDecimal.valueOf(20), BigDecimal.valueOf(30)
                                    ),
                                    tuple(
                                            "TEMPERATURE",
                                            "°F",
                                            new BigDecimal("68.0000"),
                                            new BigDecimal("86.0000")
                                    ),
                                    tuple(
                                            "HUMIDITY",
                                            "%",
                                            BigDecimal.valueOf(40), BigDecimal.valueOf(70)
                                    )
                            );

            assertThat(upserts)
                    .allSatisfy(upsert ->
                            assertThat(upsert.occurredAt())
                                .isEqualTo(threshold.occurredAt())
                    );

            InOrder inOrder = inOrder(
                    cultivationMemberService,
                    environmentSettingPreparationService,
                    cultivationSensorService,
                    cultivationSensorTypeService,
                    environmentSettingService
            );

            inOrder.verify(cultivationMemberService)
                    .verifyManagerAccess(CULTIVATION_ID, USER_ID);

            inOrder.verify(environmentSettingPreparationService)
                    .prepare(settings);

            inOrder.verify(cultivationSensorService)
                    .register(CULTIVATION_ID, request);

            inOrder.verify(cultivationSensorTypeService)
                    .syncSensorTypes(sensor, sensorTypes);

            inOrder.verify(environmentSettingService)
                    .apply(
                            CULTIVATION_ID,
                            preparedSettings,
                            sensorTypeMap
                    );

            verifyNoMoreInteractions(
                    cultivationMemberService,
                    environmentSettingPreparationService,
                    sensorTypeService,
                    cultivationSensorService,
                    cultivationSensorTypeService,
                    environmentSettingService
            );
        }
    }

    @Nested
    @DisplayName("환경설정 수정")
    class Facade_updateEnvironmentSetting {

        @Test
        @DisplayName("관리자 권한을 확인하고 섭씨·화씨 임계값을 수정·발행")
        void updateEnvironmentSettingSuccess() {
            long celsiusSensorTypeId = 10L;
            long fahrenheitSensorTypeId = 11L;

            EnvironmentSettingRequest rawRequest = new EnvironmentSettingRequest(
                    celsiusSensorTypeId,
                    new BigDecimal("19.0"),
                    new BigDecimal("25.0")
            );

            EnvironmentSettingRequest celsiusRequest = new EnvironmentSettingRequest(
                    celsiusSensorTypeId,
                    new BigDecimal("19.0000"),
                    new BigDecimal("25.0000")
            );

            EnvironmentSettingRequest fahrenheitRequest = new EnvironmentSettingRequest(
                    fahrenheitSensorTypeId,
                    new BigDecimal("66.2000"),
                    new BigDecimal("77.0000")
            );

            SensorType temperature = new SensorType("TEMPERATURE", "°C");
            ReflectionTestUtils.setField(temperature, "id", celsiusSensorTypeId);

            SensorType fahrenheit = new SensorType("TEMPERATURE", "°F");
            ReflectionTestUtils.setField(fahrenheit, "id", fahrenheitSensorTypeId);

            List<EnvironmentSettingRequest> preparedRequests =
                    List.of(celsiusRequest, fahrenheitRequest);

            Map<Long, SensorType> sensorTypeMap = Map.of(
                    celsiusSensorTypeId, temperature,
                    fahrenheitSensorTypeId, fahrenheit
            );

            when(environmentSettingPreparationService.prepare(List.of(rawRequest)))
                    .thenReturn(new PreparedEnvironmentSettings(
                            preparedRequests,
                            sensorTypeMap
                    ));

            cultivationSensorFacade.updateEnvironmentSetting(USER_ID, CULTIVATION_ID, rawRequest);

            InOrder inOrder = inOrder(
                    cultivationMemberService,
                    environmentSettingPreparationService,
                    environmentSettingService,
                    eventPublisher
            );
            inOrder.verify(cultivationMemberService).verifyManagerAccess(CULTIVATION_ID, USER_ID);
            inOrder.verify(environmentSettingPreparationService).prepare(List.of(rawRequest));
            inOrder.verify(environmentSettingService).updateExisting(CULTIVATION_ID, celsiusRequest);
            inOrder.verify(environmentSettingService).updateExisting(CULTIVATION_ID, fahrenheitRequest);

            ArgumentCaptor<ThresholdInfoEvent> eventCaptor = ArgumentCaptor.forClass(ThresholdInfoEvent.class);
            inOrder.verify(eventPublisher).publishEvent(eventCaptor.capture());

            ThresholdInfoEvent event = eventCaptor.getValue();
            assertThat(event.cultivationId()).isEqualTo(CULTIVATION_ID);
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
                                    "°C",
                                    new BigDecimal("19.0000"),
                                    new BigDecimal("25.0000")
                            ),
                            tuple(
                                    "TEMPERATURE",
                                    "°F",
                                    new BigDecimal("66.2000"),
                                    new BigDecimal("77.0000")
                            )
                    );
            assertThat(event.occurredAt()).isNotNull();

            verifyNoMoreInteractions(
                    cultivationMemberService,
                    environmentSettingPreparationService,
                    environmentSettingService,
                    eventPublisher
            );
            verifyNoInteractions(
                    sensorTypeService,
                    cultivationSensorService,
                    cultivationSensorTypeService
            );
        }
    }

    @Nested
    @DisplayName("삭제")
    class Facade_delete {

        /**
         * when(cultivationMemberService.existingMember(
         * CULTIVATION_ID,
         * USER_ID
         * )).thenReturn(true);
         */
        @Test
        @DisplayName("개별 삭제 성공")
        // 접근 권한 확인 후 센서 삭제
        void delete_success() {

            List<CultivationSensorTypeResponse> sensorTypes = List.of(
                    new CultivationSensorTypeResponse(10L, "TEMPERATURE", "C"),
                    new CultivationSensorTypeResponse(20L, "HUMIDITY", "%")
            );

            CultivationSensorResponse sensorResponse = new CultivationSensorResponse(
                    SENSOR_ID,
                    "EUI-001",
                    "MODEL-A",
                    "배양실 센서",
                    "ROOM-1",
                    "북쪽 선반",
                    SensorConnectStatus.OFFLINE,
                    sensorTypes
            );

            when(cultivationSensorService.findById(CULTIVATION_ID, SENSOR_ID))
                    .thenReturn(sensorResponse);

            cultivationSensorFacade.delete(USER_ID, CULTIVATION_ID, SENSOR_ID);

            ArgumentCaptor<SensorInfoDeleteEvent> eventCaptor =
                    ArgumentCaptor.forClass(SensorInfoDeleteEvent.class);

            InOrder inOrder = inOrder(
                    cultivationMemberService,
                    cultivationSensorService,
                    eventPublisher
            );

            inOrder.verify(cultivationMemberService).verifyManagerAccess(CULTIVATION_ID, USER_ID);

            inOrder.verify(cultivationSensorService).findById(CULTIVATION_ID, SENSOR_ID);

            inOrder.verify(cultivationSensorService).delete(CULTIVATION_ID, SENSOR_ID);

            inOrder.verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getAllValues()).extracting(
                    SensorInfoDeleteEvent::cultivationId,
                    SensorInfoDeleteEvent::deviceEui,
                    SensorInfoDeleteEvent::sensorType,
                    SensorInfoDeleteEvent::unit
            )
            .containsExactly(
                    tuple(
                            CULTIVATION_ID,
                            "EUI-001",
                            "TEMPERATURE",
                            "C"
                    ),
                    tuple(
                            CULTIVATION_ID,
                            "EUI-001",
                            "HUMIDITY",
                            "%"
                    )
            );

            verifyNoMoreInteractions(
                    cultivationMemberService,
                    cultivationSensorService,
                    eventPublisher
            );

            verifyNoInteractions(
                    sensorTypeService,
                    cultivationSensorTypeService,
                    environmentSettingService
            );
            verifyNoMoreInteractions(cultivationMemberService, cultivationSensorService, eventPublisher);
            verifyNoInteractions(sensorTypeService, cultivationSensorTypeService, environmentSettingService);
        }
    }

    @Nested
    @DisplayName("경작 종료시 센서 전체 삭제")
    class Facade_deleteAll {

        @Test
        @DisplayName("삭제되지 않은 모든 센서를 삭제하고 임계값 및 센서 삭제 이벤트를 발행한다")
        void deleteAll_success() {
            CultivationSensorResponse sensor1 = new CultivationSensorResponse(
                    100L,
                    "EUI-001",
                    "MODEL-A",
                    "1번 센서",
                    "ROOM-1",
                    "북쪽",
                    SensorConnectStatus.ONLINE,
                    List.of(
                            new CultivationSensorTypeResponse(
                                    10L, "TEMPERATURE", "C"
                            ),
                            new CultivationSensorTypeResponse(
                                    20L, "HUMIDITY", "%"
                            )
                    )
            );

            CultivationSensorResponse sensor2 = new CultivationSensorResponse(
                    200L,
                    "EUI-002",
                    "MODEL-B",
                    "2번 센서",
                    "ROOM-2",
                    "남쪽",
                    SensorConnectStatus.OFFLINE,
                    List.of(
                            new CultivationSensorTypeResponse(
                                    30L, "CO2", "ppm"
                            )
                    )
            );

            when(cultivationSensorService.findAll(CULTIVATION_ID))
                    .thenReturn(List.of(sensor1, sensor2));

            // when
            cultivationSensorFacade.deleteAll(USER_ID, CULTIVATION_ID);

            // then: ONLINE/OFFLINE과 관계없이 삭제되지 않은 센서 모두 삭제
            InOrder serviceOrder = inOrder(cultivationSensorService);

            serviceOrder.verify(cultivationSensorService)
                    .findAll(CULTIVATION_ID);
            serviceOrder.verify(cultivationSensorService)
                    .delete(CULTIVATION_ID, 100L);
            serviceOrder.verify(cultivationSensorService)
                    .delete(CULTIVATION_ID, 200L);

            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);

            verify(eventPublisher, times(4))
                    .publishEvent(eventCaptor.capture());

            List<Object> events = eventCaptor.getAllValues();

            // 구현상 임계값 전체 삭제 이벤트가 먼저 발행됨
            assertThat(events.getFirst()).isInstanceOf(ThresholdInfoEvent.class);

            ThresholdInfoEvent thresholdEvent =
                    (ThresholdInfoEvent) events.getFirst();

            assertThat(thresholdEvent.cultivationId())
                    .isEqualTo(CULTIVATION_ID);
            assertThat(thresholdEvent.sensorRangeList())
                    .isEmpty();

            List<SensorInfoDeleteEvent> deleteEvents = events.stream()
                    .filter(SensorInfoDeleteEvent.class::isInstance)
                    .map(SensorInfoDeleteEvent.class::cast)
                    .toList();

            assertThat(deleteEvents)
                    .extracting(
                            SensorInfoDeleteEvent::cultivationId,
                            SensorInfoDeleteEvent::deviceEui,
                            SensorInfoDeleteEvent::sensorType,
                            SensorInfoDeleteEvent::unit
                    )
                    .containsExactly(
                            tuple(CULTIVATION_ID, "EUI-001", "TEMPERATURE", "C"),
                            tuple(CULTIVATION_ID, "EUI-001", "HUMIDITY", "%"),
                            tuple(CULTIVATION_ID, "EUI-002", "CO2", "ppm")
                    );

            // 한 번의 종료 작업에서 생성된 이벤트는 같은 기준 시각 사용
            assertThat(deleteEvents)
                    .allSatisfy(event ->
                            assertThat(event.occurredAt())
                                    .isEqualTo(thresholdEvent.occurredAt())
                    );
        }

        @Test
        @DisplayName("등록된 센서가 없어도 임계값 삭제 이벤트를 발행하고 정상 처리한다")
        void deleteAll_successWhenSensorNotFound() {
            // given
            when(cultivationSensorService.findAll(CULTIVATION_ID))
                    .thenReturn(List.of());

            // when & then
            assertThatCode(() ->
                    cultivationSensorFacade.deleteAll(USER_ID, CULTIVATION_ID)
            ).doesNotThrowAnyException();

            verify(cultivationSensorService)
                    .findAll(CULTIVATION_ID);

            verify(cultivationSensorService, never())
                    .delete(anyLong(), anyLong());

            ArgumentCaptor<ThresholdInfoEvent> eventCaptor =
                    ArgumentCaptor.forClass(ThresholdInfoEvent.class);

            verify(eventPublisher)
                    .publishEvent(eventCaptor.capture());

            ThresholdInfoEvent event = eventCaptor.getValue();

            assertThat(event.cultivationId())
                    .isEqualTo(CULTIVATION_ID);
            assertThat(event.sensorRangeList())
                    .isEmpty();
            assertThat(event.occurredAt())
                    .isNotNull();

            verifyNoMoreInteractions(
                    cultivationSensorService,
                    eventPublisher
            );
        }
    }

    @Nested
    @DisplayName("조회")
    class Facade_findAll {

        @Test
        @DisplayName("조회 성공 - 멤버면 누구나 가능")
        void findAll_success() {
            when(cultivationSensorService.findAll(CULTIVATION_ID)).thenReturn(List.of());
            when(environmentSettingService.findAll(CULTIVATION_ID)).thenReturn(List.of());

            cultivationSensorFacade.findAll(USER_ID, CULTIVATION_ID);

            verify(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID, null);
            verifyNoMoreInteractions(cultivationMemberService);
        }

        @Test
        @DisplayName("조회 실패 - 멤버가 아니면 차단됨")
        void findAll_failNotMember() {
            doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                    .when(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID, null);

            assertThatThrownBy(() -> cultivationSensorFacade.findAll(USER_ID, CULTIVATION_ID))
                    .isInstanceOf(CultivationAccessDeniedException.class);

            verifyNoInteractions(cultivationSensorService, environmentSettingService);
        }

        @Test
        @DisplayName("조회 성공 - 관리자는 멤버가 아니어도 가능")
        void findAll_successForAdmin() {
            when(cultivationSensorService.findAll(CULTIVATION_ID)).thenReturn(List.of());
            when(environmentSettingService.findAll(CULTIVATION_ID)).thenReturn(List.of());

            cultivationSensorFacade.findAll(USER_ID, CULTIVATION_ID, "ADMIN");

            verify(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID, "ADMIN");
            verifyNoMoreInteractions(cultivationMemberService);
        }
    }

    @Nested
    @DisplayName("권한 차단")
    class Facade_accessDenied {

        @Test
        @DisplayName("등록 실패 - MEMBER 권한이면 차단됨")
        void register_failMemberRole() {
            doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                    .when(cultivationMemberService).verifyManagerAccess(CULTIVATION_ID, USER_ID);

            CreateCultivationSensorRequest request = new CreateCultivationSensorRequest(
                    "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반",
                    List.of(new EnvironmentSettingRequest(10L, BigDecimal.valueOf(20), BigDecimal.valueOf(30)))
            );

            assertThatThrownBy(() -> cultivationSensorFacade.register(USER_ID, CULTIVATION_ID, request))
                    .isInstanceOf(CultivationAccessDeniedException.class);

            verifyNoInteractions(sensorTypeService, cultivationSensorService, cultivationSensorTypeService, environmentSettingService, eventPublisher);
        }

        @Test
        @DisplayName("삭제 실패 - MEMBER 권한이면 차단됨")
        void delete_failMemberRole() {
            doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                    .when(cultivationMemberService).verifyManagerAccess(CULTIVATION_ID, USER_ID);

            assertThatThrownBy(() -> cultivationSensorFacade.delete(USER_ID, CULTIVATION_ID, SENSOR_ID))
                    .isInstanceOf(CultivationAccessDeniedException.class);

            verifyNoInteractions(cultivationSensorService, eventPublisher);
        }
    }
}
