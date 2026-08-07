package site.yesaido.cultivation_server.sensor.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.cultivation_server.cultivation.dto.harvest.response.EnvironmentComplianceResponse;
import site.yesaido.cultivation_server.cultivation.entity.cultivation.Cultivation;
import site.yesaido.cultivation_server.cultivation.exception.CultivationNotFoundException;
import site.yesaido.cultivation_server.cultivation.repository.cultivation.CultivationRepository;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.repository.EnvironmentSettingRepository;
import site.yesaido.cultivation_server.sensor.repository.InfluxSensorQueryRepository;
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
    private final EnvironmentSettingRepository environmentSettingRepository;
    private final InfluxSensorQueryRepository influxSensorQueryRepository;
    private final CultivationRepository cultivationRepository;

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getCompliance(Long cultivationId) {
        Cultivation cultivation = cultivationRepository.findById(cultivationId)
                .orElseThrow(() -> new CultivationNotFoundException(cultivationId));

        LocalDate startDate = (cultivation.getStartedAt() != null ? cultivation.getStartedAt() : cultivation.getCreatedAt()).toLocalDate();
        LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        return getComplianceForPeriod(cultivationId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getDailyCompliance(Long cultivationId, LocalDate date) {
        return getComplianceForPeriod(cultivationId, date, date);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentComplianceResponse getComplianceForPeriod(Long cultivationId, LocalDate startDate, LocalDate endDate) {
        List<EnvironmentSetting> settings = environmentSettingRepository.findAllByCultivationId(cultivationId);

        Map<String, BigDecimal> rates = new HashMap<>();
        for (EnvironmentSetting setting : settings) {
            String type = setting.getSensorType().getType();

            long total = influxSensorQueryRepository.countTotal(cultivationId, type, startDate, endDate);
            if (total == 0) {
                continue;
            }

            long inRange = influxSensorQueryRepository.countInRange(cultivationId, type, startDate, endDate, setting.getThresholdMin(), setting.getThresholdMax());
            rates.put(type, rate(inRange, total));
        }

        return new EnvironmentComplianceResponse(
                rates.get("TEMPERATURE"),
                rates.get("HUMIDITY"),
                rates.get("CO2"),
                rates.get("LIGHT")
        );
    }

    // Helper Method
    private BigDecimal rate(long inRangeCount, long total) {
        return BigDecimal.valueOf(inRangeCount)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
