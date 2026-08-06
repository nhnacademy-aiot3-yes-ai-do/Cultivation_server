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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        ensureStatExists(event.cultivationId(), sensorType);

        boolean inRange = isInRange(event.value(), setting.getThresholdMin(), setting.getThresholdMax());
        if (inRange) {
            environmentComplianceStatRepository.incrementInRange(event.cultivationId(), sensorType.getId());
        } else {
            environmentComplianceStatRepository.incrementOutOfRange(event.cultivationId(), sensorType.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getCompliance(Long cultivationId) {
        List<EnvironmentComplianceStat> stats = environmentComplianceStatRepository.findAllByCultivationId(cultivationId);

        Map<String, BigDecimal> rateByType = stats.stream()
                .collect(Collectors.toMap(
                        s -> s.getSensorType().getType(),
                        this::complianceRate
                ));

        return new EnvironmentComplianceResponse(
                rateByType.get("TEMPERATURE"),
                rateByType.get("HUMIDITY"),
                rateByType.get("CO2"),
                rateByType.get("LIGHT")
        );
    }

    // Helper Method
    private void ensureStatExists(Long cultivationId, SensorType sensorType) {
        boolean exists = environmentComplianceStatRepository.findByCultivationIdAndSensorType_Id(cultivationId, sensorType.getId()).isPresent();

        if (!exists) {
            try {
                environmentComplianceStatRepository.save(new EnvironmentComplianceStat(cultivationId, sensorType));
            } catch (DataIntegrityViolationException e) {

            }
        }
    }

    private boolean isInRange(Double value, BigDecimal thresholdMin, BigDecimal thresholdMax) {
        BigDecimal threshold = BigDecimal.valueOf(value);
        return threshold.compareTo(thresholdMin) >= 0 && threshold.compareTo(thresholdMax) <= 0;

    }

    private BigDecimal complianceRate(EnvironmentComplianceStat stat) {
        long total = stat.getInRangeCount() + stat.getOutOfRangeCount();
        if (total == 0) {
            return null;
        }

        return BigDecimal.valueOf(stat.getInRangeCount())
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
