package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentComplianceStat;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentComplianceStatRepository;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.repository.SensorTypeRepository;
import site.yesaido.cultivation_server.sensor.service.EnvironmentComplianceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnvironmentComplianceServiceImpl implements EnvironmentComplianceService {
    private final EnvironmentComplianceStatRepository environmentComplianceStatRepository;
    private final EnvironmentSettingRepository environmentSettingRepository;
    private final SensorTypeRepository sensorTypeRepository;

    @Override
    @Transactional
    public void recordCount(SensorValueEvent event) {
        SensorType sensorType = sensorTypeRepository.findByType(event.sensorType().name()).orElse(null);
        if (sensorType == null) {
            return;
        }

        EnvironmentSetting setting = environmentSettingRepository.findByCultivationIdAndSensorType_Id(event.cultivationId(), sensorType.getId())
                .orElse(null);
        if (setting == null) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        ensureStatExists(event.cultivationId(), sensorType, today);

        boolean inRange = isInRange(event.value(), setting.getThresholdMin(), setting.getThresholdMax());
        if (inRange) {
            environmentComplianceStatRepository.incrementInRange(event.cultivationId(), sensorType.getId(), today);
        } else {
            environmentComplianceStatRepository.incrementOutOfRange(event.cultivationId(), sensorType.getId(), today);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getCompliance(Long cultivationId) {
        List<EnvironmentComplianceStat> stats = environmentComplianceStatRepository.findAllByCultivationId(cultivationId);
        return toResponse(sumByType(stats));
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getDailyCompliance(Long cultivationId, LocalDate date) {
        List<EnvironmentComplianceStat> stats = environmentComplianceStatRepository.findAllByCultivationIdAndStatDate(cultivationId, date);
        return toResponse(sumByType(stats));
    }

    // Helper Method
    private void ensureStatExists(Long cultivationId, SensorType sensorType, LocalDate date) {
        boolean exists = environmentComplianceStatRepository.findByCultivationIdAndSensorType_IdAndStatDate(cultivationId, sensorType.getId(), date).isPresent();

        if (!exists) {
            try {
                environmentComplianceStatRepository.save(new EnvironmentComplianceStat(cultivationId, sensorType, date));
            } catch (DataIntegrityViolationException e) {

            }
        }
    }

    private boolean isInRange(Double value, BigDecimal thresholdMin, BigDecimal thresholdMax) {
        BigDecimal threshold = BigDecimal.valueOf(value);
        return threshold.compareTo(thresholdMin) >= 0 && threshold.compareTo(thresholdMax) <= 0;

    }

    private Map<String, int[]> sumByType(List<EnvironmentComplianceStat> stats) {
        Map<String, int[]> totals = new HashMap<>();
        for (EnvironmentComplianceStat stat : stats) {
            String type = stat.getSensorType().getType();
            int[] counts = totals.computeIfAbsent(type, k -> new int[2]);
            counts[0] += stat.getInRangeCount();
            counts[1] += stat.getOutOfRangeCount();
        }
        return totals;
    }

    private BigDecimal rateOf(int[] counts) {
        if (counts == null) {
            return null;
        }
        return rate(counts[0], counts[1]);
    }

    private BigDecimal rate(int inRangeCount, int outOfRangeCount) {
        long total = inRangeCount + outOfRangeCount;
        if (total == 0) {
            return null;
        }

        return BigDecimal.valueOf(inRangeCount)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private EnvironmentComplianceResponse toResponse(Map<String, int[]> totalsByType) {
        return new EnvironmentComplianceResponse(
                rateOf(totalsByType.get("TEMPERATURE")),
                rateOf(totalsByType.get("HUMIDITY")),
                rateOf(totalsByType.get("CO2")),
                rateOf(totalsByType.get("LIGHT"))
        );
    }
}
