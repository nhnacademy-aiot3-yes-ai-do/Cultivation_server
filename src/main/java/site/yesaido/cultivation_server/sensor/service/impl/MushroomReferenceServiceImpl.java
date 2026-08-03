package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceRequest;
import site.yesaido.cultivation_server.sensor.dto.request.MushroomReferenceThresholdRequest;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.MushroomReferenceInfoResponse;
import site.yesaido.cultivation_server.sensor.entity.MushroomReference;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.MushroomReferenceNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceRepository;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceThresholdRepository;
import site.yesaido.cultivation_server.sensor.service.MushroomReferenceService;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MushroomReferenceServiceImpl implements MushroomReferenceService {
    private final MushroomReferenceRepository mushroomReferenceRepository;
    private final MushroomReferenceThresholdRepository mushroomReferenceThresholdRepository;

    private final SensorTypeService sensorTypeService;

    @Override
    @Transactional
    public long registerMushroomReference(MushroomReferenceRequest dto) {
        if(mushroomReferenceRepository.existsMushroomReferenceByMushroomScientificName(dto.mushroomScientificName())){
            throw new MushroomReferenceAlreadyExistException("mushroom-scientific-name:%s".formatted(dto.mushroomScientificName()));
        }

        MushroomReference registerMushroomReference = MushroomReference.create(dto);
        MushroomReference saveMushroomReference = mushroomReferenceRepository.saveAndFlush(registerMushroomReference);

        List<MushroomReferenceThresholdRequest> thresholds = dto.thresholds();

        List<Long> sensorTypeIds = thresholds.stream().map(MushroomReferenceThresholdRequest::sensorTypeId).toList();

        Map<Long, SensorType> sensorTypeMap = sensorTypeService.getSensorTypeList(sensorTypeIds).stream()
                .collect(Collectors.toMap(SensorType::getId, st -> st));

        List<MushroomReferenceThreshold> mushroomReferenceThresholds = createMushroomReferenceThreshold(saveMushroomReference, thresholds, sensorTypeMap);

        mushroomReferenceThresholdRepository.saveAll(mushroomReferenceThresholds);

        return saveMushroomReference.getId();
    }

    @Override
    @Transactional
    public void updateMushroomReference(long mushroomReferenceId, MushroomReferenceRequest dto) {
        if(!mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)) {
            throw new MushroomReferenceNotFoundException("id:%s".formatted(mushroomReferenceId));
        }

        // mushroom reference 업데이트
        MushroomReference mushroomReference = mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId);

        if(!dto.mushroomNameKo().equals(mushroomReference.getMushroomNameKo())) {
            mushroomReference.setMushroomNameKo(dto.mushroomNameKo());
        }
        if(!dto.mushroomNameEn().equals(mushroomReference.getMushroomNameEn())) {
            mushroomReference.setMushroomNameEn(dto.mushroomNameEn());
        }
        if(!dto.mushroomScientificName().equals(mushroomReference.getMushroomScientificName())) {
            mushroomReference.setMushroomScientificName(dto.mushroomScientificName());
        }

        // threshold 업데이트 필요 준비
        List<MushroomReferenceThresholdRequest> thresholdRequests = dto.thresholds();

        Map<Long, MushroomReferenceThresholdRequest> thresholdUpdateRequests = thresholdRequests.stream()
                .filter(t -> Objects.nonNull(t.id()))
                .collect(Collectors.toMap(MushroomReferenceThresholdRequest::id, t -> t));

        List<MushroomReferenceThresholdRequest> thresholdRegisterRequests = thresholdRequests.stream()
                .filter(t -> Objects.isNull(t.id()))
                .toList();

        Set<Long> thresholdUpdateKeys = thresholdUpdateRequests.keySet();

        Set<MushroomReferenceThreshold> mushroomReferenceThresholds = mushroomReference.getMushroomReferenceThresholds();

        Set<MushroomReferenceThreshold> deleteThresholds = mushroomReferenceThresholds.stream()
                .filter(t -> !thresholdUpdateKeys.contains(t.getId()))
                .collect(Collectors.toSet());

        Map<Long, MushroomReferenceThreshold> updateThresholds = mushroomReferenceThresholds.stream()
                .filter(t -> thresholdUpdateKeys.contains(t.getId()))
                .collect(Collectors.toMap(MushroomReferenceThreshold::getId, t -> t));

        // threshold 삭제
        mushroomReference.getMushroomReferenceThresholds().removeAll(deleteThresholds);
        mushroomReferenceThresholdRepository.deleteAll(deleteThresholds);

        // 필요한 센서 타입 가져오기 (전부 - 수정사항만 가져오기엔 로직이 복잡도 증가)
        Set<Long> needSensorTypeIds = thresholdRequests.stream()
                .map(MushroomReferenceThresholdRequest::sensorTypeId)
                .collect(Collectors.toSet());

        Map<Long, SensorType> sensorTypeMap = sensorTypeService.getSensorTypeList(needSensorTypeIds.stream()
                        .filter(Objects::nonNull)
                        .toList()).stream()
                .collect(Collectors.toMap(SensorType::getId, s -> s));

        // threshold 업데이트
        for (Long id : thresholdUpdateKeys){
            MushroomReferenceThreshold threshold = updateThresholds.get(id);
            MushroomReferenceThresholdRequest request = thresholdUpdateRequests.get(id);

            if(!request.thresholdMin().equals(threshold.getThresholdMin())) {
                threshold.setThresholdMin(request.thresholdMin());
            }
            if(!request.thresholdMax().equals(threshold.getThresholdMax())) {
                threshold.setThresholdMax(request.thresholdMax());
            }
            if(!request.sensorTypeId().equals(threshold.getSensorType().getId())) {
                threshold.setSensorType(sensorTypeMap.get(request.sensorTypeId()));
            }
        }

        // threshold 생성
        List<MushroomReferenceThreshold> saveThresholds = createMushroomReferenceThreshold(mushroomReference, thresholdRegisterRequests, sensorTypeMap);

        mushroomReferenceThresholdRepository.saveAll(saveThresholds);
    }

    public List<MushroomReferenceThreshold> createMushroomReferenceThreshold(MushroomReference mushroomReference, List<MushroomReferenceThresholdRequest> thresholdRequests, Map<Long, SensorType> sensorTypeMap) {
        List<MushroomReferenceThreshold> saveThresholds = new ArrayList<>();
        for (MushroomReferenceThresholdRequest request : thresholdRequests) {
            SensorType sensorType = sensorTypeMap.get(request.sensorTypeId());

            MushroomReferenceThreshold threshold = MushroomReferenceThreshold.create(sensorType, mushroomReference, request);

            saveThresholds.add(threshold);
        }

        return saveThresholds;
    }

    @Override
    @Transactional
    public void deleteMushroomReference(long mushroomReferenceId) {
        if(!mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)) {
            throw new MushroomReferenceNotFoundException("id:%s".formatted(mushroomReferenceId));
        }

        MushroomReference mushroomReference = mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId);
        mushroomReference.disconnect();

        Set<MushroomReferenceThreshold> mushroomReferenceThresholds = mushroomReference.getMushroomReferenceThresholds();

        mushroomReferenceThresholdRepository.deleteAll(mushroomReferenceThresholds);
        mushroomReferenceRepository.delete(mushroomReference);
    }

    @Override
    @Transactional(readOnly = true)
    public MushroomReferenceInfoResponse getMushroomReferenceInfo(long mushroomReferenceId) {
        if(!mushroomReferenceRepository.existsMushroomReferenceById(mushroomReferenceId)) {
            throw new MushroomReferenceNotFoundException("id:%s".formatted(mushroomReferenceId));
        }

        MushroomReference mushroomReference = mushroomReferenceRepository.findMushroomReferenceById(mushroomReferenceId);
        return MushroomReferenceInfoResponse.from(mushroomReference);
    }

    @Override
    @Transactional(readOnly = true)
    public MushroomReferenceInfoListResponse getAllMushroomReferenceInfoList() {
        List<MushroomReference> allMushroomReference = mushroomReferenceRepository.findAllMushroomReference();

        List<MushroomReferenceInfoResponse> mushroomReferenceInfoResponseList = allMushroomReference.stream()
                .map(MushroomReferenceInfoResponse::from)
                .toList();
        return new MushroomReferenceInfoListResponse(mushroomReferenceInfoResponseList);
    }

}
