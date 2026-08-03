package site.yesaido.cultivation_server.cultivation.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.cultivation.dto.environmentsetting.EnvironmentRange;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidRangeValidatorTest {
    private final ValidRangeValidator validator = new ValidRangeValidator();

    @Test
    @DisplayName("min이 max보다 작으면 유효하다")
    void validWhenMinLessThanMax() {
        EnvironmentRange range = new EnvironmentRange(BigDecimal.valueOf(10), BigDecimal.valueOf(20));

        boolean result = validator.isValid(range, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("min과 max가 같으면 유효하다")
    void validWhenMinEqualsMax() {
        EnvironmentRange range = new EnvironmentRange(BigDecimal.valueOf(15), BigDecimal.valueOf(15));

        boolean result = validator.isValid(range, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("min이 max보다 크면 유효하지 않다")
    void invalidWhenMinGreaterThanMax() {
        EnvironmentRange range = new EnvironmentRange(BigDecimal.valueOf(30), BigDecimal.valueOf(20));

        boolean result = validator.isValid(range, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("environmentRange가 null이면 유효하다 (다른 @NotNull 등에서 처리)")
    void validWhenEnvironmentRangeIsNull() {
        boolean result = validator.isValid(null, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("min이 null이면 유효하다 (@NotNull에서 별도 처리)")
    void validWhenMinIsNull() {
        EnvironmentRange range = new EnvironmentRange(null, BigDecimal.valueOf(20));

        boolean result = validator.isValid(range, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("max가 null이면 유효하다 (@NotNull에서 별도 처리)")
    void validWhenMaxIsNull() {
        EnvironmentRange range = new EnvironmentRange(BigDecimal.valueOf(10), null);

        boolean result = validator.isValid(range, null);

        assertThat(result).isTrue();
    }
}
