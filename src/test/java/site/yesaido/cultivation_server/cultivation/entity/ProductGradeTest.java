package site.yesaido.cultivation_server.cultivation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductGradeTest {

    @ParameterizedTest(name = "점수 {0}점은 {1} 등급으로 분류됨")
    @DisplayName("경계값 포함 점수-등급 매핑 검증")
    @CsvSource({
            "100, TOP",
            "90, TOP",
            "89.99, HIGH",
            "75, HIGH",
            "74.99, MID",
            "50, MID",
            "49.99, LOW",
            "0, LOW"
    })
    void fromScoreMapsScoreToExpectedGrade(String score, ProductGrade expected) {
        ProductGrade grade = ProductGrade.fromScore(new BigDecimal(score));

        assertThat(grade).isEqualTo(expected);
    }
}