package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.SensorSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorFacadeImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CultivationSensorFacadeTest {

    private static final long USER_ID = 1L;
    private static final long CULTIVATION_ID = 10L;
    private static final long SENSOR_ID = 100L;

    @Mock
    CultivationRepository cultivationRepository;

    @Mock
    SensorTypeService sensorTypeService;

    @Mock
    CultivationSensorService cultivationSensorService;

    @Mock
    CultivationSensorTypeService cultivationSensorTypeService;

    @Mock
    EnvironmentSettingService environmentSettingService;

    @InjectMocks
    CultivationSensorFacadeImpl cultivationSensorFacade;

    /**
     * 1. 등록 성공
     *    → 타입 조회
     *    → 센서 등록
     *    → 타입 연결
     *    → 환경설정 적용
     *    → 센서 ID 반환
     *
     * 2. 삭제 성공
     *    → 접근 권한 확인
     *    → sensorService.delete() 호출
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

            when(cultivationRepository.isMember(CULTIVATION_ID, USER_ID)).thenReturn(true);

            when(sensorTypeService.getSensorTypeList(sensorTypeIds))
                    .thenReturn(sensorTypes);

            when(cultivationSensorService.register(CULTIVATION_ID, request)).thenReturn(sensor);

            // when
            long registeredSensorId = cultivationSensorFacade.register(USER_ID, CULTIVATION_ID, request);

            // then
            assertThat(registeredSensorId).isEqualTo(SENSOR_ID);

            InOrder inOrder = inOrder(
                    cultivationRepository,
                    sensorTypeService,
                    cultivationSensorService,
                    cultivationSensorTypeService,
                    environmentSettingService
            );

            inOrder.verify(cultivationRepository)
                    .isMember(CULTIVATION_ID, USER_ID);

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
                    cultivationRepository,
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

        @Test
        @DisplayName("삭제 성공")
        // 접근 권한 확인 후 센서 삭제
        void delete_success() {
            when(cultivationRepository.isMember(
                    CULTIVATION_ID,
                    USER_ID
            )).thenReturn(true);

            cultivationSensorFacade.delete(USER_ID, CULTIVATION_ID, SENSOR_ID);

            InOrder inOrder = inOrder(cultivationRepository, cultivationSensorService);

            inOrder.verify(cultivationRepository).isMember(CULTIVATION_ID, USER_ID);
            inOrder.verify(cultivationSensorService).delete(CULTIVATION_ID, SENSOR_ID);

            verifyNoMoreInteractions(cultivationRepository, cultivationSensorService);
            verifyNoInteractions(sensorTypeService, cultivationSensorTypeService, environmentSettingService);
        }
    }


}