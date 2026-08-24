package site.yesaido.cultivation_server.cultivation.entity.harvest;

import java.math.BigDecimal;

public enum ProductGrade {
    TOP,
    HIGH,
    MID,
    LOW;

    private static final BigDecimal TOP_THRESHOLD = BigDecimal.valueOf(90);
    private static final BigDecimal HIGH_THRESHOLD = BigDecimal.valueOf(75);
    private static final BigDecimal MID_THRESHOLD = BigDecimal.valueOf(50);

    public static ProductGrade fromScore(BigDecimal productScore) {
        if (productScore.compareTo(TOP_THRESHOLD) >= 0) {
            return TOP;
        }
        if (productScore.compareTo(HIGH_THRESHOLD) >= 0) {
            return HIGH;
        }
        if (productScore.compareTo(MID_THRESHOLD) >= 0) {
            return MID;
        }
        return LOW;
    }
}
