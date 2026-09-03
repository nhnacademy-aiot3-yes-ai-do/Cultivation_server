package site.yesaido.cultivation_server.cultivation.entity.harvest;

import java.math.BigDecimal;

public enum ProductGrade {
    TOP,
    HIGH,
    MID,
    LOW;

    private static final BigDecimal TOP_THRESHOLD = BigDecimal.valueOf(0.05);
    private static final BigDecimal HIGH_THRESHOLD = BigDecimal.valueOf(0.20);
    private static final BigDecimal MID_THRESHOLD = BigDecimal.valueOf(0.50);

    public static ProductGrade fromPercentile(BigDecimal topPercentile) {
        if (topPercentile.compareTo(TOP_THRESHOLD) <= 0) {
            return TOP;
        }
        if (topPercentile.compareTo(HIGH_THRESHOLD) <= 0) {
            return HIGH;
        }
        if (topPercentile.compareTo(MID_THRESHOLD) <= 0) {
            return MID;
        }
        return LOW;
    }
}
