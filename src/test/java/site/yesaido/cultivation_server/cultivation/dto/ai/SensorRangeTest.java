package site.yesaido.cultivation_server.cultivation.dto.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorRangeTest {

    @Test
    @DisplayName("정상 범위면 정상 생성된다")
    void createsSuccessfullyWithValidRange() {
        SensorRange range = new SensorRange(18.0, 24.0);

        assertThat(range.min()).isEqualTo(18.0);
        assertThat(range.max()).isEqualTo(24.0);
    }

    @ParameterizedTest(name = "min={0}, max={1} -> 예외 발생")
    @MethodSource("invalidRanges")
    @DisplayName("min이 null이거나 무한대이거나 min>max이면 예외를 던진다")
    void throwsOnInvalidRange(Double min, Double max) {
        assertThatThrownBy(() -> new SensorRange(min, max))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidRanges() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(null, 24.0),
                org.junit.jupiter.params.provider.Arguments.of(18.0, null),
                org.junit.jupiter.params.provider.Arguments.of(Double.POSITIVE_INFINITY, 24.0),
                org.junit.jupiter.params.provider.Arguments.of(18.0, Double.NaN),
                org.junit.jupiter.params.provider.Arguments.of(25.0, 24.0)
        );
    }
}