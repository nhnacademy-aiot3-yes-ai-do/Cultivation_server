package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

        List<SensorType> sensorTypes = List.of(temperature, humidity);

        ArgumentCaptor<List<CultivationSensorType>> captor =
                ArgumentCaptor.forClass(List.class);

        //when
        cultivationSensorTypeService.syncSensorTypes(sensor, sensorTypes);


        //then
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
}