package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.exception.DuplicateSensorTypeException;
import site.yesaido.cultivation_server.sensor.exception.InvalidThresholdRangeException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeNotFoundException;
import site.yesaido.cultivation_server.sensor.exception.SensorTypeUnitConflictException;
import site.yesaido.cultivation_server.sensor.service.EnvironmentSettingPreparationService;
import site.yesaido.cultivation_server.sensor.service.SensorTypeService;
import site.yesaido.cultivation_server.sensor.service.TemperatureThresholdConverter;
import site.yesaido.cultivation_server.sensor.service.model.PreparedEnvironmentSettings;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static site.yesaido.cultivation_server.sensor.support.SensorUnits.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentSettingPreparationServiceImpl implements EnvironmentSettingPreparationService {

    private final SensorTypeService sensorTypeService;
    private final TemperatureThresholdConverter converter;

    @Override
    public PreparedEnvironmentSettings prepare(
            List<EnvironmentSettingRequest> requests
    ) {
        validateDuplicateSensorTypeIds(requests);

        Map<Long, SensorType> requestedTypeMap =
                loadRequestedTypeMap(requests);

        // 사용자의 °C 요청을 C/F 두 개 중복 요청 차단위한 검증
        validateDuplicateTemperatureTypes(requestedTypeMap);

        Map<Long, EnvironmentSettingRequest> preparedRequests =
                new LinkedHashMap<>();

        Map<Long, SensorType> preparedTypeMap =
                new LinkedHashMap<>(requestedTypeMap);

        for (EnvironmentSettingRequest request : requests) {
            SensorType requestedType =
                    requestedTypeMap.get(request.sensorTypeId());

            if (requestedType == null) {
                throw new SensorTypeNotFoundException(
                        "sensorTypeId:%d"
                                .formatted(request.sensorTypeId())
                );
            }

            if (TEMPERATURE.equals(requestedType.getType())) {
                addTemperaturePair(
                        request,
                        requestedType,
                        preparedRequests,
                        preparedTypeMap
                );
            } else {
                preparedRequests.put(
                        request.sensorTypeId(),
                        request
                );
            }
        }

        return new PreparedEnvironmentSettings(
                List.copyOf(preparedRequests.values()),
                preparedTypeMap
        );
    }

    private Map<Long, SensorType> loadRequestedTypeMap(
            List<EnvironmentSettingRequest> requests
    ) {
        List<Long> sensorTypeIds = requests.stream()
                .map(EnvironmentSettingRequest::sensorTypeId)
                .distinct()
                .toList();

        return sensorTypeService
                .getSensorTypeList(sensorTypeIds)
                .stream()
                .collect(Collectors.toMap(
                        SensorType::getId,
                        Function.identity()
                ));
    }

    // Preparation Service가 내부적으로 °C 요청을 C/F 두 개로 확장
    private void addTemperaturePair(
            EnvironmentSettingRequest request,
            SensorType requestedType,
            Map<Long, EnvironmentSettingRequest> preparedRequests,
            Map<Long, SensorType> preparedTypeMap
    ) {
        String sourceUnit = requestedType.getValueUnit();

        SensorType celsiusType =
                sensorTypeService.getByTypeAndValueUnit(
                        TEMPERATURE,
                        CELSIUS
                );

        SensorType fahrenheitType =
                sensorTypeService.getByTypeAndValueUnit(
                        TEMPERATURE,
                        FAHRENHEIT
                );

        BigDecimal celsiusMin = converter.toCelsius(
                request.thresholdMin(),
                sourceUnit
        );

        BigDecimal celsiusMax = converter.toCelsius(
                request.thresholdMax(),
                sourceUnit
        );

        validateRange(celsiusMin, celsiusMax);

        BigDecimal fahrenheitMin =
                converter.toFahrenheit(celsiusMin);

        BigDecimal fahrenheitMax =
                converter.toFahrenheit(celsiusMax);

        preparedRequests.put(
                celsiusType.getId(),
                new EnvironmentSettingRequest(
                        celsiusType.getId(),
                        celsiusMin,
                        celsiusMax
                )
        );

        preparedRequests.put(
                fahrenheitType.getId(),
                new EnvironmentSettingRequest(
                        fahrenheitType.getId(),
                        fahrenheitMin,
                        fahrenheitMax
                )
        );

        preparedTypeMap.put(
                celsiusType.getId(),
                celsiusType
        );

        preparedTypeMap.put(
                fahrenheitType.getId(),
                fahrenheitType
        );
    }

    private void validateRange(
            BigDecimal min,
            BigDecimal max
    ) {
        if (min.compareTo(max) > 0) {
            throw new InvalidThresholdRangeException(
                    "온도 최솟값은 최댓값보다 클 수 없습니다. min:%s, max:%s"
                            .formatted(min, max)
            );
        }
    }

    private void validateDuplicateSensorTypeIds(
            List<EnvironmentSettingRequest> requests
    ) {
        Set<Long> seen = new HashSet<>();

        List<Long> duplicateIds = requests.stream()
                .map(EnvironmentSettingRequest::sensorTypeId)
                .filter(sensorTypeId -> !seen.add(sensorTypeId))
                .distinct()
                .toList();

        if (!duplicateIds.isEmpty()) {
            throw new DuplicateSensorTypeException(
                    duplicateIds
            );
        }
    }

    private void validateDuplicateTemperatureTypes(
            Map<Long, SensorType> sensorTypeMap
    ) {
        List<Long> temperatureTypeIds =
                sensorTypeMap.values().stream()
                        .filter(sensorType ->
                                TEMPERATURE.equals(
                                        sensorType.getType()
                                )
                        )
                        .map(SensorType::getId)
                        .toList();

        if (temperatureTypeIds.size() > 1) {
            throw new SensorTypeUnitConflictException(
                    "온도 환경설정은 °C 또는 °F 중 하나만 요청할 수 있습니다."
            );
        }
    }
}
