package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.CultivationSensorNotFoundException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorTypeService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CultivationSensorTypeServiceImpl implements CultivationSensorTypeService {

    private final CultivationSensorTypeRepository cultivationSensorTypeRepository;

    // 해당 센서에 활성화한 sensorType의 개수만큼 활성화
    // 기존에 있던 관계면 요청에서 온것으로만 최신화 시켜야함
    @Override
    @Transactional
    public void syncSensorTypes(CultivationSensor sensor, List<SensorType> requestedSensorTypes) {
        if (sensor.getId() == null) {
            throw new CultivationSensorNotFoundException("저장되지 않은 센서, Id미존재 -> 센서 고유Eui:%s".formatted(sensor.getDeviceEui()));
        }

        if (requestedSensorTypes.isEmpty()) {
            throw new SensorTypeNotFoundException("센서 타입을 찾을 수 없어 센서 타입 연결절차 스킵 -> 센서Id:%s 센서 고유Eui:%s"
                    .formatted(sensor.getId(), sensor.getDeviceEui()));
        }

        List<CultivationSensorType> existingCultivationSensorTypes = cultivationSensorTypeRepository.findAllByCultivationSensor_Id(sensor.getId());

        Map<Long, CultivationSensorType> existingByTypeId = existingCultivationSensorTypes.stream()
                                .collect(Collectors.toMap(
                                        CultivationSensorType::getId, Function.identity())
                                );

        Map<Long, SensorType> requestedByTypeId = requestedSensorTypes.stream()
                        .collect(Collectors.toMap(
                                SensorType::getId, Function.identity())
                        );

        // 기존에 있지만 요청에 없어 지울 타입
        List<CultivationSensorType> relationsToDelete =
                existingCultivationSensorTypes.stream()
                        .filter(relation -> !requestedByTypeId.containsKey(
                                relation.getSensorType().getId()
                        )).toList();

        // 요청에는 있지만 기존에는 없는 타입
        List<CultivationSensorType> relationsToCreate =
                requestedSensorTypes.stream()
                                .filter(type -> !existingByTypeId.containsKey(
                                        type.getId()
                                ))
                                .map(type -> new CultivationSensorType(sensor, type))
                                .toList();

        if (!relationsToDelete.isEmpty()) {
            cultivationSensorTypeRepository.deleteAll(relationsToDelete);
        }

        if (!relationsToCreate.isEmpty()) {
            cultivationSensorTypeRepository.saveAll(relationsToCreate);
        }
    }
}
