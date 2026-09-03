package site.yesaido.cultivation_server.sensor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static site.yesaido.cultivation_server.sensor.support.SensorUnits.CELSIUS;
import static site.yesaido.cultivation_server.sensor.support.SensorUnits.FAHRENHEIT;

class TemperatureThresholdConverterTest {

    private final TemperatureThresholdConverter converter =
            new TemperatureThresholdConverter();

    @Test
    @DisplayName("섭씨 입력은 소수점 넷째 자리로 정규화한다")
    void toCelsius_normalizesCelsiusScale() {
        BigDecimal result = converter.toCelsius(
                new BigDecimal("18"),
                CELSIUS
        );

        assertThat(result).isEqualTo(new BigDecimal("18.0000"));
    }

    @Test
    @DisplayName("화씨를 섭씨로 변환한다")
    void toCelsius_convertsFahrenheit() {
        BigDecimal result = converter.toCelsius(
                new BigDecimal("75.2"),
                FAHRENHEIT
        );

        assertThat(result).isEqualTo(new BigDecimal("24.0000"));
    }

    @Test
    @DisplayName("섭씨를 화씨로 변환한다")
    void toFahrenheit_convertsCelsius() {
        BigDecimal result = converter.toFahrenheit(
                new BigDecimal("18")
        );

        assertThat(result).isEqualTo(new BigDecimal("64.4000"));
    }

    @Test
    @DisplayName("지원하지 않는 온도 단위는 거부한다")
    void toCelsius_rejectsUnsupportedUnit() {
        assertThatThrownBy(() -> converter.toCelsius(
                BigDecimal.ONE,
                "K"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 온도 단위");
    }
}
