package site.yesaido.cultivation_server.rabbitmq.event;

import java.math.BigDecimal;

// 값에대한 Null 확인처리는 ruleEngine측 담당
public record SensorRange(
        String sensorType,

        String unit,

        BigDecimal minValue,
        BigDecimal maxValue
) {
}
