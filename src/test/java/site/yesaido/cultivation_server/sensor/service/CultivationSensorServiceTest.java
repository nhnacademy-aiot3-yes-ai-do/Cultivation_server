package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.exception.CultivationSensorAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.CultivationSensorNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CultivationSensorServiceTest {
    private static final long CULTIVATION_ID = 1L;
    private static final long SENSOR_ID = 10L;

    @Mock
    CultivationSensorRepository cultivationSensorRepository;

    @InjectMocks
    CultivationSensorServiceImpl cultivationSensorService;

    @Nested
    @DisplayName("센서 등록")
    class CultivationSensor_add {

        @Test
        @DisplayName("신규 센서 등록")
        void add_new_sensor() {
            CreateCultivationSensorRequest request = new CreateCultivationSensorRequest(
                    "EUI-001",
                    "MODEL-A",
                    "배양실 센서",
                    "ROOM-1",
                    "북쪽 선반",
                    List.of(
                            new EnvironmentSettingRequest(
                                    1L,
                                    BigDecimal.valueOf(20),
                                    BigDecimal.valueOf(30)
                            )
                    )
            );

            when(cultivationSensorRepository.findByCultivationIdAndDeviceEui(CULTIVATION_ID, request.deviceEui()))
                    .thenReturn(Optional.empty());

            when(cultivationSensorRepository.saveAndFlush(any(CultivationSensor.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(cultivationSensorRepository
                    .findByCultivationIdAndDeviceEui(CULTIVATION_ID, request.deviceEui()))
                    .thenReturn(Optional.empty());

            CultivationSensor result = cultivationSensorService.register(CULTIVATION_ID, request);

            verify(cultivationSensorRepository).findByCultivationIdAndDeviceEui(
                    CULTIVATION_ID,
                    request.deviceEui()
            );
            verify(cultivationSensorRepository).saveAndFlush(result);
            verifyNoMoreInteractions(cultivationSensorRepository);
        }
        @Test
        @DisplayName("신규 센서 예외")
        void add_new_sensor_race_condition() {
            CreateCultivationSensorRequest request = new CreateCultivationSensorRequest(
                    "EUI-001",
                    "MODEL-A",
                    "배양실 센서",
                    "ROOM-1",
                    "북쪽 선반",
                    List.of(
                            new EnvironmentSettingRequest(
                                    1L,
                                    BigDecimal.valueOf(20),
                                    BigDecimal.valueOf(30)
                            )
                    )
            );

            when(cultivationSensorRepository.findByCultivationIdAndDeviceEui(CULTIVATION_ID, request.deviceEui()))
                    .thenReturn(Optional.empty());

            when(cultivationSensorRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("unique violation"));

            assertThatThrownBy(() ->
                    cultivationSensorService.register(CULTIVATION_ID, request))
                    .isInstanceOf(CultivationSensorAlreadyExistException.class);

            verify(cultivationSensorRepository).findByCultivationIdAndDeviceEui(
                    CULTIVATION_ID,
                    request.deviceEui()
            );

            verify(cultivationSensorRepository).saveAndFlush(any());
            verifyNoMoreInteractions(cultivationSensorRepository);
        }

        @Test
        @DisplayName("삭제된 센서 재등록")
        void add_deleted_sensor() {
            CreateCultivationSensorRequest request = new CreateCultivationSensorRequest(
                    "EUI-001",
                    "MODEL-A",
                    "배양실 센서",
                    "ROOM-1",
                    "북쪽 선반",
                    List.of(
                            new EnvironmentSettingRequest(
                                    1L,
                                    BigDecimal.valueOf(20),
                                    BigDecimal.valueOf(30)
                            )
                    )
            );

            CultivationSensor deletedSensor = new CultivationSensor(
                    CULTIVATION_ID,
                    request.deviceEui(), // 위와 똑같은 "EUI-001"
                    "OLD-MODEL",
                    "기존 센서",
                    "OLD-ROOM",
                    "기존 위치"
            );
            deletedSensor.toDelete();

            when(cultivationSensorRepository.findByCultivationIdAndDeviceEui(CULTIVATION_ID, request.deviceEui()))
                    .thenReturn(Optional.of(deletedSensor));

            CultivationSensor result =
                    cultivationSensorService.register(CULTIVATION_ID, request);

            assertThat(result).isSameAs(deletedSensor);
            assertThat(result.isDeleted()).isFalse();
            assertThat(result.getDeviceModel())
                    .isEqualTo(request.deviceModel());
            assertThat(result.getDeviceName())
                    .isEqualTo(request.deviceName());
            assertThat(result.getLocation())
                    .isEqualTo(request.location());
            assertThat(result.getLocationDetail())
                    .isEqualTo(request.locationDetail());
            assertThat(result.getSensorStatus())
                    .isEqualTo(SensorConnectStatus.OFFLINE);

            verify(cultivationSensorRepository)
                    .findByCultivationIdAndDeviceEui(
                            CULTIVATION_ID,
                            request.deviceEui()
                    );
            verifyNoMoreInteractions(cultivationSensorRepository);
        }

    }

    @Nested
    @DisplayName("센서 삭제")
    class CultivationSensor_delete {

        @Test
        @DisplayName("활성 센서 삭제 -> isDeleted=true, status=OFFLINE")
        void delete_activated_sensor() {

            CultivationSensor sensor = new CultivationSensor(
                    CULTIVATION_ID,
                    "EUI-001",
                    "MODEL-A",
                    "배양실 센서",
                    "ROOM-1",
                    "북쪽 선반"
            );

            when(cultivationSensorRepository.findByIdAndCultivationIdAndIsDeletedFalse(SENSOR_ID, CULTIVATION_ID))
                    .thenReturn(Optional.of(sensor));


            cultivationSensorService.delete(CULTIVATION_ID, SENSOR_ID);

            assertThat(sensor.isDeleted()).isTrue();
            assertThat(sensor.getSensorStatus())
                    .isEqualTo(SensorConnectStatus.OFFLINE);

            verify(cultivationSensorRepository)
                    .findByIdAndCultivationIdAndIsDeletedFalse(
                            SENSOR_ID,
                            CULTIVATION_ID
                    );
            verifyNoMoreInteractions(cultivationSensorRepository);
        }

        @Test
        @DisplayName("없는 센서 삭제 -> CultivationSensorNotFoundException")
        void delete_no_exist_sensor_exception() {

            when(cultivationSensorRepository
                    .findByIdAndCultivationIdAndIsDeletedFalse(
                            SENSOR_ID,
                            CULTIVATION_ID
                    ))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cultivationSensorService.delete(CULTIVATION_ID, SENSOR_ID))
                    .isInstanceOf(CultivationSensorNotFoundException.class);

            verify(cultivationSensorRepository)
                    .findByIdAndCultivationIdAndIsDeletedFalse(
                            SENSOR_ID,
                            CULTIVATION_ID
                    );
            verifyNoMoreInteractions(cultivationSensorRepository);
        }
    }

}