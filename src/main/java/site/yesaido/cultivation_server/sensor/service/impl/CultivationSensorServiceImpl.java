package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.request.CreateCultivationSensorRequest;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.ReusableCultivationSensorResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.exception.CultivationSensorAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.CultivationSensorNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.CultivationSensorService;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CultivationSensorServiceImpl implements CultivationSensorService {

    private final CultivationSensorRepository cultivationSensorRepository;

    @Override
    @Transactional
    public CultivationSensor register(long cultivationId, CreateCultivationSensorRequest dto) {

        Optional<CultivationSensor> existingSensor = cultivationSensorRepository
                .findByCultivationIdAndDeviceEui(cultivationId, dto.deviceEui());

        //레이스컨디션
        //요청 A: INSERT 성공
        //요청 B: INSERT → DB UNIQUE 제약 위반
        //요청 A, B에대한 예외처리
        if (existingSensor.isEmpty()) {
            try {
                return cultivationSensorRepository.saveAndFlush(dto.toEntity(cultivationId));
            } catch (DataIntegrityViolationException e) {
                throw new CultivationSensorAlreadyExistException(
                        "동시 등록 충돌 -> cultivationId:%d, deviceEui:%s"
                                .formatted(cultivationId, dto.deviceEui())
                );
            }
        }

        // 마지막은 소프트 delete 되어있는 상태이므로 dirty check 복구
        CultivationSensor sensor = existingSensor.get();

        // deleted = false시 이미존재 판단
        if (!sensor.isDeleted()) {
            throw new CultivationSensorAlreadyExistException("해당 경작지 device eui 이미존재 -> cultivationId:%d, deviceEui:%s".formatted(cultivationId, dto.deviceEui()));
        }

        sensor.toRestore(dto.deviceModel(), dto.deviceName(), dto.location(), dto.locationDetail());

        return sensor;
    }

    @Override
    public CultivationSensorResponse findById(long cultivationId, long sensorId) {
        CultivationSensor sensor = cultivationSensorRepository.findByIdAndCultivationIdAndIsDeletedFalse(sensorId, cultivationId)
                .orElseThrow(() ->
                        new CultivationSensorNotFoundException("경작지[Id:%s]에 존재하지 않는 센서 요청 sensorId:%s".formatted(cultivationId, sensorId)));

        return CultivationSensorResponse.from(sensor);
    }

    @Override
    @Transactional
    public void delete(long cultivationId, long sensorId) {

        CultivationSensor sensor = cultivationSensorRepository.findByIdAndCultivationIdAndIsDeletedFalse(sensorId, cultivationId)
                .orElseThrow(() ->
                        new CultivationSensorNotFoundException("경작지[Id:%s]에 존재하지 않는 센서 요청 sensorId:%s".formatted(cultivationId, sensorId)));

        // 소프트 DELETE
        sensor.toDelete();
    }

    @Override
    @Transactional(readOnly = true)
    // cultivationId에 해당하는 센서정보와 센서 sensorTypes 담긴정보들 dto 반환
    public List<CultivationSensorResponse> findAll(long cultivationId) {
        return cultivationSensorRepository
                .findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(
                        cultivationId
                )
                .stream()
                .map(CultivationSensorResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReusableCultivationSensorResponse> findReusableSensors(
            Long userId,
            long excludedCultivationId
    ) {
        Map<String, ReusableCultivationSensorResponse> sensorsByEui = new LinkedHashMap<>();

        cultivationSensorRepository
                .findReusableSensorsForOwner(userId, excludedCultivationId)
                .forEach(sensor -> sensorsByEui.putIfAbsent(
                        sensor.getDeviceEui(),
                        ReusableCultivationSensorResponse.from(sensor)
                ));

        return List.copyOf(sensorsByEui.values());
    }

}
