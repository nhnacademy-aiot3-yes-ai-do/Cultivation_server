package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.request.SensorTypeRequest;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoListResponse;
import site.yesaido.cultivation_server.sensor.dto.response.SensorTypeInfoResponse;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeAlreadyExistException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.SensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SensorTypeServiceImpl implements SensorTypeService {
    private final SensorTypeRepository sensorTypeRepository;

    @Override
    @Transactional
    public long registerSensorType(SensorTypeRequest dto) {
        if(sensorTypeRepository.existsSensorTypeByTypeAndValueUnit(dto.type(), dto.valueUnit())) {
            throw new SensorTypeAlreadyExistException("type:%s, valueUnit:%s".formatted(dto.type(), dto.valueUnit()));
        }

        SensorType registerSensorType = SensorType.create(dto);
        SensorType saveSensorType = sensorTypeRepository.saveAndFlush(registerSensorType);

        return saveSensorType.getId();
    }

    public void existSensorTypeById(long sensorTypeId) {
        if(!sensorTypeRepository.existsSensorTypeById(sensorTypeId)) {
            throw new SensorTypeNotFoundException("sensorTypeId:%d".formatted(sensorTypeId));
        }
    }

    @Override
    @Transactional
    public void updateSensorType(long sensorTypeId, SensorTypeRequest dto) {
        existSensorTypeById(sensorTypeId);

        SensorType sensorType = sensorTypeRepository.findSensorTypeById(sensorTypeId);

        if(!dto.type().equals(sensorType.getType())) {
            sensorType.setType(dto.type());
        }
        if(!dto.valueUnit().equals(sensorType.getValueUnit())) {
            sensorType.setValueUnit(dto.valueUnit());
        }

        sensorTypeRepository.save(sensorType);
    }

    @Override
    @Transactional
    public void deleteSensorType(long sensorTypeId) {
        existSensorTypeById(sensorTypeId);

        sensorTypeRepository.deleteById(sensorTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public SensorTypeInfoResponse getSensorTypeById(long sensorTypeId) {
        existSensorTypeById(sensorTypeId);

        SensorType sensorType = sensorTypeRepository.findSensorTypeById(sensorTypeId);

        return SensorTypeInfoResponse.from(sensorType);
    }

    @Override
    @Transactional(readOnly = true)
    public SensorTypeInfoListResponse findAll() {
        List<SensorType> all = sensorTypeRepository.findAll();

        List<SensorTypeInfoResponse> sensorTypeInfoList = all.stream()
                .map(SensorTypeInfoResponse::from)
                .toList();
        return new SensorTypeInfoListResponse(sensorTypeInfoList);
    }

    @Override
    @Transactional
    public List<SensorType> getSensorTypeList(List<Long> sensorTypeIds) {
        if(sensorTypeIds.isEmpty()) {
            return List.of();
        }

        List<SensorType> sensorTypeList = sensorTypeRepository.findAllById(sensorTypeIds);
        if(sensorTypeList.isEmpty()) {
            String idsString = sensorTypeIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            throw new SensorTypeNotFoundException("IDs: %s".formatted(idsString));
        }

        return sensorTypeList;
    }
}
