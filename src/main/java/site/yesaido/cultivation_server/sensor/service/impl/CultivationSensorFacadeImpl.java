package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.service.CultivationMemberService;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoDeleteEvent;
import site.yesaido.cultivation_server.rabbitmq.event.SensorInfoUpsertEvent;

import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.request.SensorSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.DuplicateSensorTypeException;
import site.yesaido.cultivation_server.sensor.service.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultivationSensorFacadeImpl implements CultivationSensorFacade {

    private final CultivationMemberService cultivationMemberService;
    private final SensorTypeService sensorTypeService;
    private final CultivationSensorService cultivationSensorService;
    private final CultivationSensorTypeService cultivationSensorTypeService;
    private final EnvironmentSettingService environmentSettingService;

    private final ApplicationEventPublisher eventPublisher;

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
        validateAccess(userId, cultivationId);

        List<Long> sensorTypeIds = extractAndValidateSensorTypeIds(request.sensorSettings());

        // [예외] 등록된 센서 없으면
        List<SensorType> sensorTypes = sensorTypeService.getSensorTypeList(sensorTypeIds);
        // 위의 sensorTypes를 Map형태로 변환 (environmentSettingService.apply 매개변수 형태 때문)
        Map<Long, SensorType> sensorTypeMap = sensorTypes.stream()
                .collect(Collectors.toMap(SensorType::getId, Function.identity()));

        // 없으면 물리센서 등록
        // 있고 soft deleted 상태면 복구
        // [예외] 이미 해당 경작지에 존재하는 device eui 센서 등록시
        CultivationSensor sensor = cultivationSensorService.register(cultivationId, request);

        // 관계생성
        // [예외] 등록하려는 센서타입 없을시
        cultivationSensorTypeService.syncSensorTypes(sensor, sensorTypes);

        // [예외] 요청한 센서세팅에 정해둔 센서 타입이 없는 경우
        environmentSettingService.apply(cultivationId, request.sensorSettings(), sensorTypeMap);

        sensorTypes.stream()
                .map(sensorType -> toSensorInfoUpsertEvent(cultivationId, sensor, sensorType))
                .forEach(eventPublisher::publishEvent);


        return sensor.getId();
    }

    private SensorInfoUpsertEvent toSensorInfoUpsertEvent(long cultivationId, CultivationSensor sensor, SensorType sensorType) {
        site.yesaido.cultivation_server.rabbitmq.event.SensorType sensorTypeEnum = site.yesaido.cultivation_server.rabbitmq.event.SensorType
                .fromString(sensorType.getType());

        return new SensorInfoUpsertEvent(
                cultivationId,
                sensor.getLocation(), sensor.getLocationDetail(), sensor.getDeviceModel(), sensor.getDeviceName(), sensor.getDeviceEui(),
                sensorTypeEnum, sensorType.getValueUnit()
                );
    }

    @Override
    @Transactional
    public void delete(Long userId, long cultivationId, long sensorId) {
        validateAccess(userId, cultivationId);

        CultivationSensorResponse sensor = cultivationSensorService.findById(cultivationId, sensorId);

        List<SensorInfoDeleteEvent> events = sensor.sensorTypes().stream()
                .map(type -> new SensorInfoDeleteEvent(
                        cultivationId,
                        sensor.deviceEui(),
                        site.yesaido.cultivation_server.rabbitmq.event.SensorType
                                .fromString(type.type()),
                        type.valueUnit()
                ))
                .toList();

        cultivationSensorService.delete(cultivationId, sensorId);

        events.forEach(eventPublisher::publishEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public CultivationSensorListResponse findAll(Long userId, long cultivationId) {
        validateAccess(userId, cultivationId);

        return new CultivationSensorListResponse(
                cultivationSensorService.findAll(cultivationId), environmentSettingService.findAll(cultivationId)
        );
    }

    /**
     * cultivationMemberService.existingMember
     * //        if(!cultivationMemberService.existingMember(cultivationId, userId)) {
     * //            throw new CultivationMemberNotFoundException();
     * //        }
     */
    private void validateAccess(Long userId, long cultivationId) {


    }

    // 셋에 담아서 중복 SensorId 요청이 들어온 것들이 있으면 중복 센서타입 예외 생성
    private List<Long> extractAndValidateSensorTypeIds(List<SensorSettingRequest> settings) {

        List<Long> sensorTypeIds = settings.stream()
                .map(SensorSettingRequest::sensorTypeId)
                .toList();

        Set<Long> seen = new HashSet<>();

        List<Long> duplicateIds = sensorTypeIds.stream()
                .filter(sensorTypeId -> !seen.add(sensorTypeId))
                .distinct()
                .toList();

        if (!duplicateIds.isEmpty()) {
            throw new DuplicateSensorTypeException(duplicateIds);
        }

        return sensorTypeIds;
    }
}
