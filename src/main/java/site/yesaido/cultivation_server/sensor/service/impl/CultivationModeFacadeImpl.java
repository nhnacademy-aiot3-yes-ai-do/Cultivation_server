package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationModeChangeResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThreshold;
import site.yesaido.cultivation_server.sensor.entity.MushroomReferenceThresholdType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorTypeRepository;
import site.yesaido.cultivation_server.sensor.repository.MushroomReferenceThresholdRepository;
import site.yesaido.cultivation_server.sensor.service.CultivationModeFacade;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultivationModeFacadeImpl implements CultivationModeFacade {
    private final CultivationService cultivationService;
    private final CultivationRepository cultivationRepository;
    private final MushroomReferenceThresholdRepository mushroomReferenceThresholdRepository;
    private final CultivationSensorTypeRepository cultivationSensorTypeRepository;
    private final EnvironmentSettingService environmentSettingService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CultivationModeChangeResponse switchToHarvestMode(Long cultivationId, Long userId) {
        CultivationModeChangeResponse response = cultivationService.switchToHarvestMode(cultivationId, userId);
        applyHavestThresholds(cultivationId);
        return response;
    }

    private void applyHavestThresholds(Long cultivationId) {
        // cultivation을 찾고
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        // 그에 해당하는 버섯 참고값을 조회하고
        Long mushroomReferenceId = cultivation.getMushroomReference().getId();

        // cultivation에서 사용하고 있는 센서 타입들을 불러오고
        List<SensorType> registeredSensorTypes = cultivationSensorTypeRepository.findDistinctSensorTypesByCultivationId(cultivationId);
        if (registeredSensorTypes.isEmpty()) {
            return;
        }
        // 불러온 리스트들을 쪼개서 Set으로 아이디를 저장
        Set<Long> registeredSensorTypeIds = registeredSensorTypes.stream()
                .map(SensorType::getId)
                .collect(Collectors.toSet());

        // 현재 키우고 있는 버섯의 수확 환경을 불러오는데, 현재 cultivation에서 사용하고 있는 센서들에 대해서만 가져옴
        List<MushroomReferenceThreshold> harvestThresholds = mushroomReferenceThresholdRepository.findAllByMushroomReference_idAndThresholdType(mushroomReferenceId, MushroomReferenceThresholdType.HARVEST)
                .stream().filter(threshold -> registeredSensorTypeIds.contains(threshold.getSensorType().getId()))
                .toList();
        if (harvestThresholds.isEmpty()) {
            return;
        }

        // 수확 환경으로 변경하라는 요청을 조립
        List<EnvironmentSettingRequest> requests = harvestThresholds.stream()
                .map(threshold -> new EnvironmentSettingRequest(
                        threshold.getSensorType().getId(),
                        threshold.getThresholdMin(),
                        threshold.getThresholdMax()))
                .toList();

        Map<Long, SensorType> sensorTypeMap = harvestThresholds.stream()
                .collect(Collectors.toMap(
                        threshold -> threshold.getSensorType().getId(),
                        MushroomReferenceThreshold::getSensorType,
                        (a,b) -> a
                ));
        environmentSettingService.apply(cultivationId, requests, sensorTypeMap);

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));
        eventPublisher.publishEvent(ThresholdInfoEvent.from(cultivationId, requests, sensorTypeMap, occurredAt));
    }
}
