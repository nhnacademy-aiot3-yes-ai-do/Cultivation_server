package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoDeleteEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;
import site.yesaido.cultivation_server.rabbitmq.event.ThresholdInfoEvent;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.service.*;
import site.yesaido.cultivation_server.sensor.service.model.PreparedEnvironmentSettings;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CultivationSensorFacadeImpl implements CultivationSensorFacade {

    private final CultivationMemberService cultivationMemberService;
    private final CultivationSensorService cultivationSensorService;
    private final CultivationSensorTypeService cultivationSensorTypeService;
    private final EnvironmentSettingService environmentSettingService;

    private final ApplicationEventPublisher eventPublisher;
    private final EnvironmentSettingPreparationService environmentSettingPreparationService;

    /**
     *
     * 경작_접근권한_검사
     * 센터 타입 ID 리스트 = 요청에서 추출();
     *    센서 타입 리스트 = 센서타입 서비스에서 조회();
     *    경작 센서 = 경작센서 서비스에서 등록();
     *
     *    경작 센서 타입 연결 서비스에서 연결(
     *         경작 센서, 센서 타입 리스트
     *     );
     *    환경설정 서비스에서 설정 반영(
     *       경작 ID, 센서 추가 요청의 환경설정, 센서 타입 리스트
     *     );
     *     return 물리 센서 ID; (cultivation_sensor_id)
     */
    @Override
    @Transactional
    public long register(Long userId, long cultivationId, CreateCultivationSensorRequest request) {
        cultivationMemberService.verifyManagerAccess(cultivationId, userId);

        PreparedEnvironmentSettings prepared =
                environmentSettingPreparationService.prepare(
                        request.sensorSettings()
                );

        List<EnvironmentSettingRequest> preparedSettings =
                prepared.requests();

        Map<Long, SensorType> sensorTypeMap =
                prepared.sensorTypeMap();

        List<SensorType> preparedSensorTypes =
                preparedSettings.stream()
                        .map(setting ->
                                sensorTypeMap.get(setting.sensorTypeId())
                        )
                        .distinct()
                        .toList();


        // 없으면 물리센서 등록
        // 있고 soft deleted 상태면 복구
        // [예외] 이미 해당 경작지에 존재하는 device eui 센서 등록시
        CultivationSensor sensor = cultivationSensorService.register(cultivationId, request);

        // 관계생성
        // [예외] 등록하려는 센서타입 없을시
        cultivationSensorTypeService.syncSensorTypes(sensor, preparedSensorTypes);

        // [예외] 요청한 센서세팅에 정해둔 센서 타입이 없는 경우
        environmentSettingService.apply(cultivationId, preparedSettings, sensorTypeMap);

        // 서울 시간대 기준
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));

        // 1. 임계값 발행
        eventPublisher.publishEvent(
                ThresholdInfoEvent.from(cultivationId, preparedSettings, sensorTypeMap, occurredAt)
        );

        // 2. 센서 정보 발행(1,2 둘다 순서보장은 X)
        preparedSensorTypes.stream()
                .map(sensorType -> toSensorInfoUpsertEvent(cultivationId, sensor, sensorType, occurredAt))
                .forEach(eventPublisher::publishEvent);


        return sensor.getId();
    }

    private SensorInfoUpsertEvent toSensorInfoUpsertEvent(long cultivationId, CultivationSensor sensor, SensorType sensorType, OffsetDateTime occurredAt) {

        return new SensorInfoUpsertEvent(
                cultivationId,
                sensor.getLocation(), sensor.getLocationDetail(), sensor.getDeviceModel(), sensor.getDeviceName(), sensor.getDeviceEui(),
                sensorType.getType(), sensorType.getValueUnit(), occurredAt
        );
    }

    @Override
    @Transactional
    public void updateEnvironmentSetting(Long userId, long cultivationId, EnvironmentSettingRequest request) {
        cultivationMemberService.verifyManagerAccess(cultivationId, userId);

        PreparedEnvironmentSettings preparedEnvironmentSettings =
                environmentSettingPreparationService.prepare(List.of(request));

        for (EnvironmentSettingRequest req : preparedEnvironmentSettings.requests()) {
            environmentSettingService.updateExisting(cultivationId, req);
        }

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC)
                .withOffsetSameInstant(ZoneOffset.ofHours(9));

        // 섭씨와 화씨가 모두 담긴 이벤트 발행
        eventPublisher.publishEvent(
                ThresholdInfoEvent.from(
                        cultivationId,
                        preparedEnvironmentSettings.requests(),
                        preparedEnvironmentSettings.sensorTypeMap(),
                        occurredAt
                )
        );
    }

    @Override
    @Transactional
    public void delete(Long userId, long cultivationId, long sensorId) {
        cultivationMemberService.verifyManagerAccess(cultivationId, userId);

        CultivationSensorResponse sensor = cultivationSensorService.findById(cultivationId, sensorId);

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));

        List<SensorInfoDeleteEvent> events = sensor.sensorTypes().stream()
                .map(type -> new SensorInfoDeleteEvent(
                        cultivationId,
                        sensor.deviceEui(),
                        type.type(),
                        type.valueUnit(),
                        occurredAt
                ))
                .toList();

        cultivationSensorService.delete(cultivationId, sensorId);

        events.forEach(eventPublisher::publishEvent);
    }

    @Override
    @Transactional
    // 경작 종료시 룰엔진, 데이터소스에 임계값 빈리스트로 반환해서 임계값 전부 삭제처리
    public void deleteAll(Long userId, long cultivationId) {
        List<CultivationSensorResponse> sensorList = cultivationSensorService.findAll(cultivationId);
        OffsetDateTime occurredAt =
                OffsetDateTime.now(ZoneOffset.ofHours(9));

        // 사용자가 경작지에 cultivationSensor를 등록하지않고 경작 종료한 경우
        // 기존 Stream과 forEach()가 자연스럽게 아무 작업도 하지 않음

        // 사용자가 정상적으로 cultivationSensor를 1개이상 등록해둔 상태에서 경작 종료한 경우
        List<SensorInfoDeleteEvent> sensorDeleteEvents =
                sensorList.stream()
                        .flatMap(sensor ->
                                sensor.sensorTypes().stream()
                                        .map(type -> new SensorInfoDeleteEvent(
                                                cultivationId,
                                                sensor.deviceEui(),
                                                type.type(),
                                                type.valueUnit(),
                                                occurredAt
                                        ))
                        )
                        .toList();

        // db에서 소프트delete 반영
        sensorList.forEach(sensor ->
                cultivationSensorService.delete(cultivationId, sensor.sensorId())
        );

        eventPublisher.publishEvent(
                new ThresholdInfoEvent(
                        cultivationId,
                        List.of(),
                        occurredAt
                )
        );

        sensorDeleteEvents.forEach(
                eventPublisher::publishEvent
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CultivationSensorListResponse findAll(Long userId, long cultivationId) {
        return doFindAll(userId, cultivationId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CultivationSensorListResponse findAll(Long userId, long cultivationId, String role) {
        return doFindAll(userId, cultivationId, role);
    }

    private CultivationSensorListResponse doFindAll(Long userId, long cultivationId, String role) {
        cultivationMemberService.existCultivationMember(cultivationId, userId, role);
        return new CultivationSensorListResponse(cultivationSensorService.findAll(cultivationId), environmentSettingService.findAll(cultivationId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReusableCultivationSensorListResponse findReusableSensors(
            Long userId,
            long excludedCultivationId
    ) {
        cultivationMemberService.verifyOwnerAccess(excludedCultivationId, userId);
        return new ReusableCultivationSensorListResponse(
                cultivationSensorService.findReusableSensors(userId, excludedCultivationId)
        );
    }
}
