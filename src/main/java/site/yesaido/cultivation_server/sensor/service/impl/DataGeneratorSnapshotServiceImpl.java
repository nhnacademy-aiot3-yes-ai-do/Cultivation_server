package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorSensorResponse;
import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorSnapshotResponse;
import site.yesaido.cultivation_server.sensor.dto.response.datagenerator.DataGeneratorThresholdResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.service.DataGeneratorSnapshotService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

// Data Generator 초기 복구용 센서와 임계값 snapshot을 조립합니다.
@Service
@RequiredArgsConstructor
public class DataGeneratorSnapshotServiceImpl
        implements DataGeneratorSnapshotService {

    private static final Set<CultivationStatus> ACTIVE_CULTIVATION_STATUSES =
            Set.of(CultivationStatus.CREATED, CultivationStatus.RUNNING);

    private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

    private final CultivationSensorRepository cultivationSensorRepository;
    private final EnvironmentSettingRepository environmentSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public DataGeneratorSnapshotResponse getSnapshot() {
        List<CultivationSensor> cultivationSensors
                = cultivationSensorRepository.findAllForDataGeneratorSnapshot(ACTIVE_CULTIVATION_STATUSES);

        validateUniqueDeviceEuis(cultivationSensors);

        List<DataGeneratorSensorResponse> sensorResponses = cultivationSensors.stream()
                        .map(DataGeneratorSensorResponse::from)
                        .sorted(Comparator.comparingLong(DataGeneratorSensorResponse::cultivationId)
                                .thenComparing(DataGeneratorSensorResponse::deviceEui))
                        .toList();

        List<DataGeneratorThresholdResponse> thresholdResponses = environmentSettingRepository
                        .findAllForDataGeneratorSnapshot(ACTIVE_CULTIVATION_STATUSES).stream()
                        .map(DataGeneratorThresholdResponse::from)
                        .sorted(Comparator.comparingLong(DataGeneratorThresholdResponse::cultivationId)
                                .thenComparing(DataGeneratorThresholdResponse::sensorType)
                                .thenComparing(DataGeneratorThresholdResponse::unit))
                        .toList();

        OffsetDateTime snapshotAt = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(SEOUL_OFFSET);

        return new DataGeneratorSnapshotResponse(
                snapshotAt,
                sensorResponses,
                thresholdResponses
        );
    }

    private void validateUniqueDeviceEuis(List<CultivationSensor> cultivationSensors) {
        Map<String, Long> cultivationIdByDeviceEui = new HashMap<>();

        for (CultivationSensor cultivationSensor : cultivationSensors) {
            String deviceEui = cultivationSensor.getDeviceEui();
            long cultivationId = cultivationSensor.getCultivationId();

            Long existingCultivationId = cultivationIdByDeviceEui.putIfAbsent(deviceEui, cultivationId);

            if (existingCultivationId != null) {
                throw new CustomServerException(
                        "Data Generator snapshot을 생성할 수 없습니다.",
                        "활성 재배에서 중복 deviceEui가 발견되었습니다. "
                                + "deviceEui=%s, cultivationIds=%d,%d"
                                .formatted(deviceEui, existingCultivationId, cultivationId),
                        ServerErrorLevel.ERROR_LEVEL
                );
            }
        }
    }
}