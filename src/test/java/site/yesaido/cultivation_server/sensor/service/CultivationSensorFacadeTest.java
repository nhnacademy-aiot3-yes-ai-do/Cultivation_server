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
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.SensorSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorTypeResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorFacadeImpl;

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
            SensorSettingRequest temperatureSetting =
                    new SensorSettingRequest(
                            10L,
                            BigDecimal.valueOf(20),
                            BigDecimal.valueOf(30)
                    );

            SensorSettingRequest humiditySetting =
                    new SensorSettingRequest(
                            20L,
                            BigDecimal.valueOf(40),
                            BigDecimal.valueOf(70)
                    );

            List<SensorSettingRequest> settings =
                    List.of(temperatureSetting, humiditySetting);

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
                    new SensorType("TEMPERATURE", "C");
            SensorType humidity =
                    new SensorType("HUMIDITY", "%");

            ReflectionTestUtils.setField(
                    temperature,
                    "id",
                    temperatureSetting.sensorTypeId()
            );
            ReflectionTestUtils.setField(
                    humidity,
                    "id",
                    humiditySetting.sensorTypeId()
            );

            List<Long> sensorTypeIds = List.of(10L, 20L);
            List<SensorType> sensorTypes =
                    List.of(temperature, humidity);

            Map<Long, SensorType> sensorTypeMap = Map.of(
                    10L, temperature,
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

            when(sensorTypeService.getSensorTypeList(sensorTypeIds))
                    .thenReturn(sensorTypes);

            when(cultivationSensorService.register(CULTIVATION_ID, request)).thenReturn(sensor);

            // when
            long registeredSensorId = cultivationSensorFacade.register(USER_ID, CULTIVATION_ID, request);

            // then
            assertThat(registeredSensorId).isEqualTo(SENSOR_ID);

            InOrder inOrder = inOrder(
                    cultivationMemberService,
                    sensorTypeService,
                    cultivationSensorService,
                    cultivationSensorTypeService,
                    environmentSettingService
            );

            inOrder.verify(cultivationMemberService)
                    .verifyManagerAccess(CULTIVATION_ID, USER_ID);

            ArgumentCaptor<SensorInfoUpsertEvent> eventCaptor =
                    ArgumentCaptor.forClass(SensorInfoUpsertEvent.class);

            verify(eventPublisher, times(2))
                    .publishEvent(eventCaptor.capture());

            List<SensorInfoUpsertEvent> events = eventCaptor.getAllValues();

            assertThat(events)
                    .extracting(
                            SensorInfoUpsertEvent::cultivationId,
                            SensorInfoUpsertEvent::deviceEui,
                            SensorInfoUpsertEvent::sensorType,
                            SensorInfoUpsertEvent::unit
                    )
                    .containsExactly(
                            tuple(
                                    CULTIVATION_ID,
                                    "EUI-001",
                                    site.yesaido.cultivation_server.rabbitmq.event.SensorType.TEMPERATURE,
                                    "C"
                            ),
                            tuple(
                                    CULTIVATION_ID,
                                    "EUI-001",
                                    site.yesaido.cultivation_server.rabbitmq.event.SensorType.HUMIDITY,
                                    "%"
                            )
                    );

            inOrder.verify(sensorTypeService)
                    .getSensorTypeList(sensorTypeIds);

            inOrder.verify(cultivationSensorService)
                    .register(CULTIVATION_ID, request);

            inOrder.verify(cultivationSensorTypeService)
                    .syncSensorTypes(sensor, sensorTypes);

            inOrder.verify(environmentSettingService)
                    .apply(
                            CULTIVATION_ID,
                            settings,
                            sensorTypeMap
                    );

            verifyNoMoreInteractions(
                    cultivationMemberService,
                    sensorTypeService,
                    cultivationSensorService,
                    cultivationSensorTypeService,
                    environmentSettingService
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
        @DisplayName("삭제 성공")
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
                            site.yesaido.cultivation_server.rabbitmq.event.SensorType.TEMPERATURE,
                            "C"
                    ),
                    tuple(
                            CULTIVATION_ID,
                            "EUI-001",
                            site.yesaido.cultivation_server.rabbitmq.event.SensorType.HUMIDITY,
                            "%"
                    )
            );

            verifyNoMoreInteractions(cultivationMemberService, cultivationSensorService, eventPublisher);
            verifyNoInteractions(sensorTypeService, cultivationSensorTypeService, environmentSettingService);
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

            verify(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID);
            verifyNoMoreInteractions(cultivationMemberService);
        }

        @Test
        @DisplayName("조회 실패 - 멤버가 아니면 차단됨")
        void findAll_failNotMember() {
            doThrow(new CultivationAccessDeniedException(CULTIVATION_ID))
                    .when(cultivationMemberService).existCultivationMember(CULTIVATION_ID, USER_ID);

            assertThatThrownBy(() -> cultivationSensorFacade.findAll(USER_ID, CULTIVATION_ID))
                    .isInstanceOf(CultivationAccessDeniedException.class);

            verifyNoInteractions(cultivationSensorService, environmentSettingService);
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
                    List.of(new SensorSettingRequest(10L, BigDecimal.valueOf(20), BigDecimal.valueOf(30)))
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