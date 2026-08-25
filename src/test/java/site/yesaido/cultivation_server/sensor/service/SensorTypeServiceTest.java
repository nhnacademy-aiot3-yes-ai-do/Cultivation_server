package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeInUseException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
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
    @DisplayName("동일 타입과 단위의 센서 타입 등록은 conflict다")
    void registerSensorTypeWhenAlreadyExists() {
        SensorTypeRequest request = new SensorTypeRequest("test-type", "test-unit");
        when(sensorTypeRepository.existsSensorTypeByTypeAndValueUnit(request.type(), request.valueUnit()))
                .thenReturn(true);

        Assertions.assertThrows(SensorTypeAlreadyExistException.class,
                () -> sensorTypeService.registerSensorType(request));

        verify(sensorTypeRepository, never()).saveAndFlush(any());
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
    @DisplayName("없는 센서 타입 수정은 not found다")
    void updateSensorTypeWhenNotFound() {
        long sensorTypeId = 1L;
        SensorTypeRequest request = new SensorTypeRequest("new-type", "new-unit");
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(false);

        Assertions.assertThrows(SensorTypeNotFoundException.class,
                () -> sensorTypeService.updateSensorType(sensorTypeId, request));

        verify(sensorTypeRepository, never()).findSensorTypeById(anyLong());
        verify(sensorTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("센서 타입 삭제")
    void deleteSensorType() {
        long sensorTypeId = 1L;

        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);

        sensorTypeService.deleteSensorType(sensorTypeId);

        verify(sensorTypeRepository).existsSensorTypeById(sensorTypeId);
        verify(sensorTypeRepository).deleteById(sensorTypeId);
        verify(sensorTypeRepository).flush();
    }

    @Test
    @DisplayName("없는 센서 타입 삭제는 not found다")
    void deleteSensorTypeWhenNotFound() {
        long sensorTypeId = 1L;
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(false);

        Assertions.assertThrows(SensorTypeNotFoundException.class,
                () -> sensorTypeService.deleteSensorType(sensorTypeId));

        verify(sensorTypeRepository, never()).existsInUseById(anyLong());
        verify(sensorTypeRepository, never()).deleteById(anyLong());
        verify(sensorTypeRepository, never()).flush();
    }

    @Test
    @DisplayName("사용 중인 센서 타입은 삭제할 수 없다")
    void deleteSensorTypeWhenInUse() {
        long sensorTypeId = 1L;
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);
        when(sensorTypeRepository.existsInUseById(sensorTypeId)).thenReturn(true);

        Assertions.assertThrows(SensorTypeInUseException.class,
                () -> sensorTypeService.deleteSensorType(sensorTypeId));

        verify(sensorTypeRepository).existsInUseById(sensorTypeId);
        verify(sensorTypeRepository, never()).deleteById(sensorTypeId);
    }

    @Test
    @DisplayName("삭제 중 FK 제약 위반이 발생하면 사용 중 conflict로 변환한다")
    void deleteSensorTypeWhenForeignKeyViolationOccurs() {
        long sensorTypeId = 1L;
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);
        when(sensorTypeRepository.existsInUseById(sensorTypeId)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("foreign key violation"))
                .when(sensorTypeRepository).flush();

        Assertions.assertThrows(SensorTypeInUseException.class,
                () -> sensorTypeService.deleteSensorType(sensorTypeId));

        verify(sensorTypeRepository).deleteById(sensorTypeId);
        verify(sensorTypeRepository).flush();
    }

    @Test
    @DisplayName("삭제 직전 다른 요청이 센서 타입을 삭제하면 not found다")
    void deleteSensorTypeWhenAlreadyDeletedConcurrently() {
        long sensorTypeId = 1L;
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(true);
        when(sensorTypeRepository.existsInUseById(sensorTypeId)).thenReturn(false);
        doThrow(new EmptyResultDataAccessException(1)).when(sensorTypeRepository).deleteById(sensorTypeId);

        Assertions.assertThrows(SensorTypeNotFoundException.class,
                () -> sensorTypeService.deleteSensorType(sensorTypeId));

        verify(sensorTypeRepository).deleteById(sensorTypeId);
        verify(sensorTypeRepository, never()).flush();
    }

    @Test
    @DisplayName("없는 센서 타입 단건 조회는 not found다")
    void getSensorTypeByIdWhenNotFound() {
        long sensorTypeId = 1L;
        when(sensorTypeRepository.existsSensorTypeById(sensorTypeId)).thenReturn(false);

        Assertions.assertThrows(SensorTypeNotFoundException.class,
                () -> sensorTypeService.getSensorTypeById(sensorTypeId));

        verify(sensorTypeRepository, never()).findSensorTypeById(anyLong());
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

    @Test
    @DisplayName("요청한 센서 타입이 모두 없으면 not found다")
    void getSensorTypeListWhenAllRequestedTypesAreMissing() {
        List<Long> sensorTypeIds = List.of(1L, 2L);
        when(sensorTypeRepository.findAllById(sensorTypeIds)).thenReturn(List.of());

        Assertions.assertThrows(SensorTypeNotFoundException.class,
                () -> sensorTypeService.getSensorTypeList(sensorTypeIds));
    }

    @Test
    @DisplayName("빈 센서 타입 ID 목록 조회는 빈 목록을 반환한다")
    void getSensorTypeListWhenIdsAreEmpty() {
        Assertions.assertEquals(List.of(), sensorTypeService.getSensorTypeList(List.of()));
        verifyNoInteractions(sensorTypeRepository);
    }
}
