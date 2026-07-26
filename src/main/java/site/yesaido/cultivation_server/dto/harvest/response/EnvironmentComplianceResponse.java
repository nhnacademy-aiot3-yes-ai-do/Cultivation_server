package site.yesaido.cultivation_server.dto.harvest.response;

import java.math.BigDecimal;

public record EnvironmentComplianceResponse(
        BigDecimal temperatureCompliance,
        BigDecimal humidityCompliance,
        BigDecimal co2Compliance,
        BigDecimal lightCompliance
) {
}
