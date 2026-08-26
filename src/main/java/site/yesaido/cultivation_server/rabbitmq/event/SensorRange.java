package site.yesaido.cultivation_server.rabbitmq.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import site.yesaido.cultivation_server.sensor.exception.InvalidSensorRangeEventException;

import java.math.BigDecimal;
import java.util.Locale;

// 값에대한 Null 확인처리는 ruleEngine측 담당
public record SensorRange(
        @NotBlank
        String sensorType,

        @NotBlank
        String unit,

        @NotNull
        BigDecimal minValue,

        @NotNull
        BigDecimal maxValue
) {

    public SensorRange {
        if (sensorType == null) {
            throw invalid("sensorType이 null입니다.");
        }

        if (unit == null) {
            throw invalid("unit이 null입니다.");
        }

        if (minValue == null) {
            throw invalid("minValue가 null입니다.");
        }

        if (maxValue == null) {
            throw invalid("maxValue가 null입니다.");
        }

        sensorType = sensorType.trim().toUpperCase(Locale.ROOT);
        unit = unit.trim();

        if (minValue.compareTo(maxValue) > 0) {
            throw invalid(
                    "minValue가 maxValue보다 큽니다. min=%s, max=%s"
                            .formatted(minValue, maxValue)
            );
        }
    }

    private static InvalidSensorRangeEventException invalid (String content){
        return new InvalidSensorRangeEventException(content);
    }
}
