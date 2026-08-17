package site.yesaido.cultivation_server.sensor.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.impl.CultivationSensorTypeServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 검증흐름
 * CultivationSensor 1개
 * + SensorType 2개
 * → CultivationSensorType 2개 생성
 * → saveAll() 한 번 호출
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CultivationSensorTypeServiceTest {

    //given

    @Mock
    CultivationSensorTypeRepository cultivationSensorTypeRepository;

    @InjectMocks
    CultivationSensorTypeServiceImpl cultivationSensorTypeService;

    @Test
    @DisplayName("경작 센서와 선택한 센서 타입들을 연결하여 저장")
    @SuppressWarnings("unchecked")
    void connectSuccess() {
        CultivationSensor sensor = new CultivationSensor(
                1L,
                "EUI-001",
                "MODEL-A",
                "배양실 센서",
                "ROOM-1",
                "북쪽 선반"
        );
        SensorType temperature = new SensorType("TEMPERATURE", "C");
        SensorType humidity = new SensorType("HUMIDITY", "%");

        ReflectionTestUtils.setField(sensor, "id", 100L);
        ReflectionTestUtils.setField(temperature, "id", 10L);
        ReflectionTestUtils.setField(humidity, "id", 20L);

        List<SensorType> sensorTypes = List.of(temperature, humidity);

        when(cultivationSensorTypeRepository.findAllByCultivationSensor_Id(100L))
                .thenReturn(List.of());

        ArgumentCaptor<List<CultivationSensorType>> captor =
                ArgumentCaptor.forClass(List.class);

        //when
        cultivationSensorTypeService.syncSensorTypes(sensor, sensorTypes);


        //then
        verify(cultivationSensorTypeRepository)
                .findAllByCultivationSensor_Id(100L);
        verify(cultivationSensorTypeRepository).saveAll(captor.capture());

        List<CultivationSensorType> savedCultivationSensorTypes = captor.getValue();
        assertThat(savedCultivationSensorTypes).hasSize(2);

        assertThat(savedCultivationSensorTypes)
                .extracting(CultivationSensorType::getCultivationSensor)
                .containsOnly(sensor);

        assertThat(savedCultivationSensorTypes)
                .extracting(CultivationSensorType::getSensorType)
                .containsExactlyInAnyOrder(
                        temperature,
                        humidity
                );

        verifyNoMoreInteractions(cultivationSensorTypeRepository);
    }

    @Test
    @DisplayName("이미 연결된 센서 타입은 중복 저장하지 않음")
    void doesNotCreateDuplicateRelation() {
        CultivationSensor sensor = new CultivationSensor(
                1L,
                "EUI-001",
                "MODEL-A",
                "배양실 센서",
                "ROOM-1",
                "북쪽 선반"
        );
        SensorType temperature = new SensorType("TEMPERATURE", "C");
        CultivationSensorType existingRelation =
                new CultivationSensorType(sensor, temperature);

        ReflectionTestUtils.setField(sensor, "id", 100L);
        ReflectionTestUtils.setField(temperature, "id", 10L);
        ReflectionTestUtils.setField(existingRelation, "id", 999L);

        when(cultivationSensorTypeRepository.findAllByCultivationSensor_Id(100L))
                .thenReturn(List.of(existingRelation));

        cultivationSensorTypeService.syncSensorTypes(sensor, List.of(temperature));

        verify(cultivationSensorTypeRepository)
                .findAllByCultivationSensor_Id(100L);
        verifyNoMoreInteractions(cultivationSensorTypeRepository);
    }
}
