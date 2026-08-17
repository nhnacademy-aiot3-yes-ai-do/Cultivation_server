package site.yesaido.cultivation_server.rabbitmq.event;

import java.time.LocalDate;

public record EnvironmentComplianceRequest(
        Long cultivationId,
        LocalDate startDate,
        LocalDate endDate
) {
}
