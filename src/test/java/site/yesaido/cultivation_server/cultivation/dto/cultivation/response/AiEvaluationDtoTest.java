package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.cultivation.dto.ai.AiEvaluationDto;
import site.yesaido.cultivation_server.cultivation.exception.InvalidEvaluationRangeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiEvaluationDtoTest {

    @Test
    @DisplayName("정상 범위(1~5)면 정상 생성된다")
    void createsSuccessfullyWithinRange() {
        AiEvaluationDto dto = new AiEvaluationDto(1, 5, "건조함에 취약", "전략");

        assertThat(dto.difficultyLevel()).isEqualTo(1);
        assertThat(dto.growthSpeed()).isEqualTo(5);
    }

    @Test
    @DisplayName("difficultyLevel이 범위를 벗어나면 예외를 던진다")
    void throwsWhenDifficultyLevelOutOfRange() {
        assertThatThrownBy(() -> new AiEvaluationDto(0, 3, "s", "a"))
                .isInstanceOf(InvalidEvaluationRangeException.class);
        assertThatThrownBy(() -> new AiEvaluationDto(6, 3, "s", "a"))
                .isInstanceOf(InvalidEvaluationRangeException.class);
    }

    @Test
    @DisplayName("growthSpeed가 범위를 벗어나면 예외를 던진다")
    void throwsWhenGrowthSpeedOutOfRange() {
        assertThatThrownBy(() -> new AiEvaluationDto(3, 0, "s", "a"))
                .isInstanceOf(InvalidEvaluationRangeException.class);
        assertThatThrownBy(() -> new AiEvaluationDto(3, 6, "s", "a"))
                .isInstanceOf(InvalidEvaluationRangeException.class);
    }
}