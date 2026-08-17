package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.SensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.impl.SensorTypeServiceImpl;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorTypeServiceTest {

    @Mock
    private SensorTypeRepository sensorTypeRepository;

    @InjectMocks
    private SensorTypeServiceImpl sensorTypeService;

    @Test
    @DisplayName("센서 타입 등록")
    void registerSensorType() {
        SensorTypeRequest request = new SensorTypeRequest("test-type", "test-unit");
        SensorType sensorType = SensorType.create(request);

        long setId = 1L;
        ReflectionTestUtils.setField(sensorType, "id", setId);

        when(sensorTypeRepository.existsSensorTypeByTypeAndValueUnit(anyString(), anyString())).thenReturn(false);
        when(sensorTypeRepository.saveAndFlush(any(SensorType.class))).thenReturn(sensorType);

        long id = sensorTypeService.registerSensorType(request);

        Assertions.assertEquals(setId, id);

        verify(sensorTypeRepository).existsSensorTypeByTypeAndValueUnit(request.type(), request.valueUnit());

        ArgumentCaptor<SensorType> captor = ArgumentCaptor.forClass(SensorType.class);
        verify(sensorTypeRepository).saveAndFlush(captor.capture());

        SensorType captured = captor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(request.type(), captured.getType()),
                () -> Assertions.assertEquals(request.valueUnit(), captured.getValueUnit())
        );
    }

    @Test
    @DisplayName("센서 타입 수정")
    void updateSensorType() {
        long sensorTypeId = 1L;
        SensorType sensorType = SensorType.create(new SensorTypeRequest("old-type", "old-unit"));
        ReflectionTestUtils.setField(sensorType, "id", sensorTypeId);

        SensorTypeRequest newRequest = new SensorTypeRequest("new-type", "new-unit");

        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);
        when(sensorTypeRepository.findSensorTypeById(sensorTypeId)).thenReturn(sensorType);

        sensorTypeService.updateSensorType(sensorTypeId, newRequest);

        Assertions.assertAll(
                () -> Assertions.assertEquals(newRequest.type(), sensorType.getType()),
                () -> Assertions.assertEquals(newRequest.valueUnit(), sensorType.getValueUnit())
        );

        verify(sensorTypeRepository).existsSensorTypeById(sensorTypeId);
        verify(sensorTypeRepository).findSensorTypeById(sensorTypeId);
        verify(sensorTypeRepository).save(sensorType);
    }

    @Test
    @DisplayName("센서 타입 삭제")
    void deleteSensorType() {
        long sensorTypeId = 1L;

        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);

        sensorTypeService.deleteSensorType(sensorTypeId);

        verify(sensorTypeRepository).existsSensorTypeById(sensorTypeId);
        verify(sensorTypeRepository).deleteById(sensorTypeId);
    }

    @Test
    @DisplayName("센서 타입 전체 조회")
    void findAllSensorType() {
        SensorType sensorType1 = SensorType.create(new SensorTypeRequest("type-1", "unit-1"));
        ReflectionTestUtils.setField(sensorType1, "id", 1L);
        SensorType sensorType2 = SensorType.create(new SensorTypeRequest("type-2", "unit-2"));
        ReflectionTestUtils.setField(sensorType2, "id", 2L);

        when(sensorTypeRepository.findAll()).thenReturn(List.of(sensorType1, sensorType2));

        SensorTypeInfoListResponse result = sensorTypeService.findAll();

        SensorTypeInfoListResponse expected = new SensorTypeInfoListResponse(
                List.of(SensorTypeInfoResponse.from(sensorType1), SensorTypeInfoResponse.from(sensorType2))
        );

        Assertions.assertEquals(expected, result);

        verify(sensorTypeRepository).findAll();
    }

    @Test
    @DisplayName("특정 센서 타입들 조회")
    void findSensorTypeByIds() {
        List<Long> ids = List.of(1L, 2L);

        SensorType sensorType1 = SensorType.create(new SensorTypeRequest("type-1", "unit-1"));
        ReflectionTestUtils.setField(sensorType1, "id", 1L);
        SensorType sensorType2 = SensorType.create(new SensorTypeRequest("type-2", "unit-2"));
        ReflectionTestUtils.setField(sensorType2, "id", 2L);

        when(sensorTypeRepository.findAllById(ids)).thenReturn(List.of(sensorType1, sensorType2));

        List<SensorType> result = sensorTypeService.getSensorTypeList(ids);

        Assertions.assertEquals(List.of(sensorType1, sensorType2), result);

        verify(sensorTypeRepository).findAllById(ids);
    }
}
