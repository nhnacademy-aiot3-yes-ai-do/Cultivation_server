package site.yesaido.cultivation_server.rabbitmq.event;

import java.math.BigDecimal;

public record HarvestCompletedPayload(
        String cultivationName,
        BigDecimal harvestWeight
) {
}
