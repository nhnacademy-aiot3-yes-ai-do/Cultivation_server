package site.yesaido.cultivation_server.cultivation.dto.harvest.response;

import java.math.BigDecimal;

public record EnvironmentComplianceResponse(
        BigDecimal temperatureCompliance,
        BigDecimal humidityCompliance,
        BigDecimal co2Compliance,
        BigDecimal lightCompliance
) {
}
