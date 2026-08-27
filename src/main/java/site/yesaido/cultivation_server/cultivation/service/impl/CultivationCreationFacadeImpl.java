package site.yesaido.cultivation_server.cultivation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.request.CultivationCreateRequest;
import site.yesaido.cultivation_server.cultivation.dto.cultivation.response.CultivationCreateResponse;
import site.yesaido.cultivation_server.cultivation.service.CultivationCreationFacade;
import site.yesaido.cultivation_server.cultivation.service.CultivationService;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingService;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultivationCreationFacadeImpl implements CultivationCreationFacade {

    private final CultivationService cultivationService;
    private final SensorTypeService sensorTypeService;
    private final EnvironmentSettingService environmentSettingService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CultivationCreateResponse create(Long userId, CultivationCreateRequest request) {
        List<EnvironmentSettingRequest> settings = request.environmentSettingRequests();
        // 요청한 각 임계값 센서의 고유번호별 센서타입 매칭
        Map<Long, SensorType> sensorTypeMap = toMapSensorType(settings);

        CultivationCreateResponse cultivationResponse = cultivationService.create(request, userId);

        environmentSettingService.apply(
                cultivationResponse.cultivationId(),
                settings,
                sensorTypeMap);

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.ofHours(9));

        ThresholdInfoEvent thresholdEvent = ThresholdInfoEvent.from(
                        cultivationResponse.cultivationId(),
                        settings,
                        sensorTypeMap,
                        occurredAt
                );

        eventPublisher.publishEvent(thresholdEvent);

        return cultivationResponse;
    }

    private Map<Long, SensorType> toMapSensorType(List<EnvironmentSettingRequest> settings) {
        List<Long> sensorTypeIds = settings.stream()
                .map(EnvironmentSettingRequest::sensorTypeId)
                .toList();

        List<SensorType> sensorTypes = sensorTypeService.getSensorTypeList(sensorTypeIds);

        return sensorTypes.stream()
                .collect(Collectors.toMap(
                        SensorType::getId,
                        Function.identity()
                ));
    }
}
