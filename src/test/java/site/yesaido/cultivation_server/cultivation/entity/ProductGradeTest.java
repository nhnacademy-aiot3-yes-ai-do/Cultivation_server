package site.yesaido.cultivation_server.cultivation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductGradeTest {

    @ParameterizedTest(name = "상위 비율 {0}은 {1} 등급으로 분류됨")
    @DisplayName("경계값 포함 상위 비율-등급 매핑 검증")
    @CsvSource({
            "0, TOP",
            "0.05, TOP",
            "0.0501, HIGH",
            "0.20, HIGH",
            "0.2001, MID",
            "0.50, MID",
            "0.5001, LOW",
            "1, LOW"
    })
    void fromPercentileMapsRatioToExpectedGrade(String topPercentile, ProductGrade expected) {
        ProductGrade grade = ProductGrade.fromPercentile(new BigDecimal(topPercentile));

        assertThat(grade).isEqualTo(expected);
    }
}