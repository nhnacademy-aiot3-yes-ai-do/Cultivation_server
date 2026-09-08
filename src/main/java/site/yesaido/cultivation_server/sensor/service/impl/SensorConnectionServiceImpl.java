package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.rabbitmq.event.SensorDataUnavailableEvent;
import site.yesaido.cultivation_server.sensor.dto.response.influx.LatestSensorValueResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.SensorConnectStatus;
import site.yesaido.cultivation_server.sensor.repository.CultivationSensorRepository;
import site.yesaido.cultivation_server.sensor.service.SensorConnectionService;
import site.yesaido.cultivation_server.sensor.support.SensorUnits;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorConnectionServiceImpl implements SensorConnectionService {

    private final CultivationSensorRepository cultivationSensorRepository;
    private final ApplicationEventPublisher eventPublisher;

    /*
    1. 측정값을 EUI별로 모읍니다. 다른 경작지 데이터, 필수값 누락, 판단 시각 이후 데이터는 제외합니다.
    2. DB의 센서를 기준으로 순회하며 등록된 타입·단위에 해당하는 데이터만 사용합니다. 여러 타입 중 하나라도 최근에 측정되었다면 기기는 ONLINE입니다.
    3. 이번 결과가 비어 있어도 기존 lastMeasuredAt으로 시간 초과를 판단합니다. 처음부터 측정이 없으면 OFFLINE을 유지합니다.
    4. Repository의 조건부 UPDATE로 저장합니다. updated == 0이면 변경 없이 다음 주기에 다시 판단합니다. 여기서는 엔티티 변경 메서드나 save()를 함께 호출하지 않습니다.
     */
    @Transactional
    @Override
    public void synchronize(long cultivationId, List<LatestSensorValueResponse> points, Instant checkedAt) {
        Objects.requireNonNull(points, "points");
        Objects.requireNonNull(checkedAt, "checkedAt");
        Instant now = Instant.now();

        // 조회 시작 후 10초 넘게 지연된 결과는 이번 상태 판단에서 제외
        if (checkedAt.isAfter(now) || checkedAt.plusSeconds(10).isBefore(now)) {
            return;
        }
        // String -> deviceEui, 측정값을 EUI별로 모음
        Map<String, List<LatestSensorValueResponse>> pointsByEui = points.stream()
                .filter(Objects::nonNull)
                .filter(p -> Objects.equals(p.cultivationId(), cultivationId))
                .filter(p -> p.deviceEui() != null && p.measuredAt() != null && p.value() != null)
                .filter(p -> p.sensorType() != null && p.unit() != null)
                .filter(p -> !p.measuredAt().isAfter(checkedAt))
                .collect(Collectors.groupingBy(LatestSensorValueResponse::deviceEui));


        List<CultivationSensor> sensors = cultivationSensorRepository
                .findAllByCultivationIdAndIsDeletedFalseOrderByCreatedAtAsc(cultivationId);

        for (CultivationSensor sensor : sensors) {
            synchronizeSensor(cultivationId, sensor, pointsByEui, checkedAt);
        }
    }

    private void synchronizeSensor(long cultivationId, CultivationSensor sensor,
                                   Map<String, List<LatestSensorValueResponse>> pointsByEui,
                                   Instant checkedAt) {
        Instant startedAt = sensor.getMonitoringStartedAt();
        Instant previous = sensor.getLastMeasuredAt();

        if (startedAt.isAfter(checkedAt) || (previous != null && previous.isAfter(checkedAt))) {
            return;
        }

        Instant latest = pointsByEui.getOrDefault(sensor.getDeviceEui(), List.of()).stream()
                .filter(p -> !p.measuredAt().isBefore(startedAt))
                .filter(p -> sensor.getCultivationSensorTypes().stream()
                        .map(link -> link.getSensorType())
                        .anyMatch(type -> Objects.equals(type.getType(), p.sensorType())
                                && Objects.equals(SensorUnits.normalize(type.getValueUnit()),
                                SensorUnits.normalize(p.unit()))))
                .map(LatestSensorValueResponse::measuredAt)
                .max(Instant::compareTo)
                .orElse(null);

        if (previous != null && (latest == null || previous.isAfter(latest))) {
            latest = previous;
        }
        Instant cutoff = checkedAt.minusSeconds(30);
        SensorConnectStatus nextStatus =
                latest != null && !latest.isBefore(startedAt) && latest.isAfter(cutoff)
                        ? SensorConnectStatus.ONLINE : SensorConnectStatus.OFFLINE;

        if (nextStatus == sensor.getSensorStatus() && Objects.equals(previous, latest)) {
            return;
        }

        SensorConnectStatus previousStatus = sensor.getSensorStatus();
        int updated = cultivationSensorRepository.updateConnectionIfUnchanged(
                sensor.getId(), startedAt, previousStatus.name(),
                previous, nextStatus.name(), latest);

        if (updated == 1 && previousStatus == SensorConnectStatus.ONLINE && nextStatus == SensorConnectStatus.OFFLINE) {
            eventPublisher.publishEvent(new SensorDataUnavailableEvent(
                    UUID.randomUUID(), cultivationId, sensor.getDeviceName(),
                    "30초 이상 센서 측정값이 수신되지 않았습니다.", checkedAt.atOffset(ZoneOffset.UTC)));
        }

        log.debug("[SensorConnection] sensorId={}, proposedStatus={}, updatedRows={}",
                sensor.getId(), nextStatus, updated);
    }
}
