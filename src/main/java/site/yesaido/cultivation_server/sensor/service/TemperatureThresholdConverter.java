package site.yesaido.cultivation_server.sensor.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// double을 쓰지 않기 때문에 경계값 비교에서 부동소수점 오차가 발생 X
@Component
public class TemperatureThresholdConverter {

    private static final BigDecimal THIRTY_TWO = new BigDecimal("32");
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal NINE = new BigDecimal("9");
    private static final int SCALE = 4;

    // 섭씨 변환
    public BigDecimal toCelsius(BigDecimal value, String unit) {
        return switch (unit) {
            case "°C" -> scale(value);
            case "°F" -> value.subtract(THIRTY_TWO)
                    .multiply(FIVE)
                    .divide(NINE, SCALE, RoundingMode.HALF_UP); // 연산 후 소수점 넷째 자리까지 반올림
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 온도 단위: " + unit
            );
        };
    }

    public BigDecimal toFahrenheit(BigDecimal celsius) {
        return celsius.multiply(NINE)
                .divide(FIVE, SCALE, RoundingMode.HALF_UP)
                .add(THIRTY_TWO)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
