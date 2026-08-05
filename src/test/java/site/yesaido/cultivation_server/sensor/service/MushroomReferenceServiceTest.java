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
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceThresholdRepository;
import site.yesaido.cultivation_server.sensor.service.impl.MushroomReferenceServiceImpl;
import site.yesaido.cultivation_server.sensor.service.impl.SensorTypeServiceImpl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MushroomReferenceServiceTest {

    @Mock
    private MushroomReferenceRepository mushroomReferenceRepository;

    @Mock
    private MushroomReferenceThresholdRepository mushroomReferenceThresholdRepository;

    @Mock
    private SensorTypeServiceImpl sensorTypeService;

    @InjectMocks
    private MushroomReferenceServiceImpl mushroomReferenceService;

    @Test
    @DisplayName("버섯 참조 등록")
    void registerMushroomReference() {
        MushroomReferenceThresholdRequest thresholdRequest =
                new MushroomReferenceThresholdRequest(null, 1L, BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0));

        MushroomReferenceRequest request = new MushroomReferenceRequest("test-ko-name", "test-en-name", "test-scientific-name", List.of(thresholdRequest));

        MushroomReference mushroomReference = MushroomReference.create(request);
        long setId = 1L;
        ReflectionTestUtils.setField(mushroomReference, "id", setId);

        SensorType sensorType = SensorType.create(new SensorTypeRequest("test-type", "test-unit"));
        ReflectionTestUtils.setField(sensorType, "id", 1L);

        when(mushroomReferenceRepository.existsMushroomReferenceByMushroomScientificName(request.mushroomScientificName())).thenReturn(false);
        when(mushroomReferenceRepository.saveAndFlush(any(MushroomReference.class))).thenReturn(mushroomReference);
        when(sensorTypeService.getSensorTypeList(List.of(1L))).thenReturn(List.of(sensorType));

        long id = mushroomReferenceService.registerMushroomReference(request);

        Assertions.assertEquals(setId, id);

        verify(mushroomReferenceRepository).existsMushroomReferenceByMushroomScientificName(request.mushroomScientificName());
        verify(mushroomReferenceRepository).saveAndFlush(any(MushroomReference.class));
        verify(sensorTypeService).getSensorTypeList(List.of(1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MushroomReferenceThreshold>> captor = ArgumentCaptor.forClass((Class<List<MushroomReferenceThreshold>>) (Class<?>) List.class);
        verify(mushroomReferenceThresholdRepository).saveAll(captor.capture());
        Assertions.assertEquals(1, captor.getValue().size());
    }

    @Test
    @DisplayName("버섯 참조 수정")
    void updateMushroomReference() {
        long mushroomReferenceId = 1L;

        // 기존 상태
        MushroomReferenceThresholdRequest oldThresholdRequest = new MushroomReferenceThresholdRequest(null, 1L, BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0));
        MushroomReferenceRequest oldRequest = new MushroomReferenceRequest("old-name-ko", "old-name-en", "old-scientific-name", List.of(oldThresholdRequest));
        MushroomReference mushroomReference = MushroomReference.create(oldRequest);
        ReflectionTestUtils.setField(mushroomReference, "id", mushroomReferenceId);

        SensorType sensorType = SensorType.create(new SensorTypeRequest("test-type", "test-unit"));
        ReflectionTestUtils.setField(sensorType, "id", 1L);

        MushroomReferenceThreshold existingThreshold = MushroomReferenceThreshold.create(sensorType, mushroomReference, oldThresholdRequest);
        long thresholdId = 1L;
        ReflectionTestUtils.setField(existingThreshold, "id", thresholdId);

        Set<MushroomReferenceThreshold> thresholds = new HashSet<>();
        thresholds.add(existingThreshold);
        ReflectionTestUtils.setField(mushroomReference, "mushroomReferenceThresholds", thresholds);

        // 수정 요청 (기존 threshold id 포함, 값만 변경)
        MushroomReferenceThresholdRequest updateThresholdRequest = new MushroomReferenceThresholdRequest(thresholdId, 1L, BigDecimal.valueOf(15.0), BigDecimal.valueOf(35.0));
        MushroomReferenceRequest newRequest = new MushroomReferenceRequest("new-name-ko", "new-name-en", "new-scientific-name", List.of(updateThresholdRequest));

        when(mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)).thenReturn(true);
        when(mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId)).thenReturn(mushroomReference);
        when(sensorTypeService.getSensorTypeList(List.of(1L))).thenReturn(List.of(sensorType));

        mushroomReferenceService.updateMushroomReference(mushroomReferenceId, newRequest);

        Assertions.assertAll(
                () -> Assertions.assertEquals(newRequest.mushroomNameKo(), mushroomReference.getMushroomNameKo()),
                () -> Assertions.assertEquals(newRequest.mushroomNameEn(), mushroomReference.getMushroomNameEn()),
                () -> Assertions.assertEquals(newRequest.mushroomScientificName(), mushroomReference.getMushroomScientificName()),
                () -> Assertions.assertEquals(updateThresholdRequest.thresholdMin(), existingThreshold.getThresholdMin()),
                () -> Assertions.assertEquals(updateThresholdRequest.thresholdMax(), existingThreshold.getThresholdMax())
        );

        verify(mushroomReferenceRepository).existsMushroomReferenceById(mushroomReferenceId);
        verify(mushroomReferenceRepository).findMushroomReferenceById(mushroomReferenceId);
        verify(mushroomReferenceThresholdRepository).deleteAll(anySet());
        verify(mushroomReferenceThresholdRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("버섯 참조 삭제")
    void deleteMushroomReference() {
        long mushroomReferenceId = 1L;

        MushroomReferenceRequest request = new MushroomReferenceRequest("name-ko", "name-en", "scientific-name", List.of());
        MushroomReference mushroomReference = MushroomReference.create(request);
        ReflectionTestUtils.setField(mushroomReference, "id", mushroomReferenceId);

        when(mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)).thenReturn(true);
        when(mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId)).thenReturn(mushroomReference);

        mushroomReferenceService.deleteMushroomReference(mushroomReferenceId);

        verify(mushroomReferenceRepository).existsMushroomReferenceById(mushroomReferenceId);
        verify(mushroomReferenceRepository).findMushroomReferenceById(mushroomReferenceId);
        verify(mushroomReferenceThresholdRepository).deleteAll(anySet());
        verify(mushroomReferenceRepository).delete(mushroomReference);
    }

    @Test
    @DisplayName("특정 버섯 참조 조회")
    void getMushroomReferenceById() {
        long mushroomReferenceId = 1L;

        MushroomReferenceRequest request = new MushroomReferenceRequest(
                "name-ko", "name-en", "scientific-name", List.of()
        );
        MushroomReference mushroomReference = MushroomReference.create(request);
        ReflectionTestUtils.setField(mushroomReference, "id", mushroomReferenceId);

        when(mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)).thenReturn(true);
        when(mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId)).thenReturn(mushroomReference);

        MushroomReferenceInfoResponse result = mushroomReferenceService.getMushroomReferenceInfo(mushroomReferenceId);

        MushroomReferenceInfoResponse expected = MushroomReferenceInfoResponse.from(mushroomReference);

        Assertions.assertEquals(expected, result);

        verify(mushroomReferenceRepository).findMushroomReferenceById(mushroomReferenceId);
    }

    @Test
    @DisplayName("모든 버섯 참조 조회")
    void getAllMushroomReference() {
        MushroomReferenceRequest request1 = new MushroomReferenceRequest("name-ko-1", "name-en-1", "scientific-name-1", List.of());
        MushroomReference mushroomReference1 = MushroomReference.create(request1);
        ReflectionTestUtils.setField(mushroomReference1, "id", 1L);

        MushroomReferenceRequest request2 = new MushroomReferenceRequest("name-ko-2", "name-en-2", "scientific-name-2", List.of());
        MushroomReference mushroomReference2 = MushroomReference.create(request2);
        ReflectionTestUtils.setField(mushroomReference2, "id", 2L);

        when(mushroomReferenceRepository.findAllMushroomReference()).thenReturn(List.of(mushroomReference1, mushroomReference2));

        MushroomReferenceInfoListResponse result = mushroomReferenceService.getAllMushroomReferenceInfoList();

        MushroomReferenceInfoListResponse expected = new MushroomReferenceInfoListResponse(List.of(
                MushroomReferenceInfoResponse.from(mushroomReference1),
                MushroomReferenceInfoResponse.from(mushroomReference2)
        ));

        Assertions.assertEquals(expected, result);

        verify(mushroomReferenceRepository).findAllMushroomReference();
    }
}
