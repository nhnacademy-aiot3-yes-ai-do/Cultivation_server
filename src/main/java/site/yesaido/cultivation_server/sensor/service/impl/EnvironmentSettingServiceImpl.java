package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.request.SensorSettingRequest;
import site.yesaido.cultivation_server.sensor.dto.response.EnvironmentSettingResponse;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.InvalidThresholdRangeException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnvironmentSettingServiceImpl implements EnvironmentSettingService {

    private final EnvironmentSettingRepository environmentSettingRepository;

    // 환경설정 존재시 요청한 임계값 조정, 없으면 요청한 값으로 새로운 환경설정 만들고 저장
    @Override
    @Transactional
    public void apply(long cultivationId, List<SensorSettingRequest> requests, Map<Long, SensorType> sensorTypes) {
        // 센서 세팅 요청에서 id값들 저장
        Set<Long> sensorTypeIds = requests.stream()
                .map(SensorSettingRequest::sensorTypeId)
                .collect(Collectors.toSet());


        Map<Long, EnvironmentSetting> existingSettings =
                environmentSettingRepository.findAllByCultivationIdAndSensorType_IdIn(cultivationId, sensorTypeIds)
                        .stream()
                        .collect(Collectors.toMap(
                                setting ->
                                    setting.getSensorType().getId(),
                                            Function.identity()
                        ));


        // 요청에서 미리 만들어지지않은 것들 새로 저장, 기존것은 updateThreshold 변경감지로 처리
        List<EnvironmentSetting> newSettings = new ArrayList<>();

        for (SensorSettingRequest request : requests) {
            validateThresholdRange(request);

            Long sensorTypeId = request.sensorTypeId();

            SensorType sensorType = sensorTypes.get(sensorTypeId);
            // 등록되어있는 센서타입이 있는데 요청한 센서세팅에 정해둔 센서 타입이 없는 경우 -> 잘못된 요청
            if (sensorType == null) {
                throw new SensorTypeNotFoundException("sensorTypeId:%d".formatted(sensorTypeId));
            }

            EnvironmentSetting existing = existingSettings.get(sensorTypeId);

            if (existing != null) {
                existing.updateThreshold(request.thresholdMin(),
                                        request.thresholdMax()
                );
            } else {
                // 트랜잭션 종료시 더티체크 변경감지
                newSettings.add(new EnvironmentSetting(cultivationId, sensorType, request.thresholdMin(), request.thresholdMax()));
            }
        }

        if (!newSettings.isEmpty()) {
            environmentSettingRepository.saveAll(newSettings);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentSettingResponse> findAll(long cultivationId) {
        return environmentSettingRepository.findAllByCultivationId(cultivationId)
                .stream()
                .map(EnvironmentSettingResponse::from)
                .sorted(Comparator.comparing(
                        EnvironmentSettingResponse::sensorTypeId
                ))
                .toList();
    }



    // Threshold가 min이 max보다 클때 예외 검사
    private void validateThresholdRange(SensorSettingRequest request) {

        BigDecimal thresholdMin = request.thresholdMin();
        BigDecimal thresholdMax = request.thresholdMax();

        if (thresholdMin.compareTo(thresholdMax) > 0) {
            throw new InvalidThresholdRangeException("min값이 max값보다 큼 -> max:%s min:%s".formatted(thresholdMax, thresholdMin));
        }
    }
}
