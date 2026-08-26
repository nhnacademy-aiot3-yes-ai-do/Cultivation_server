package site.yesaido.cultivation_server.rabbitmq.event;

import java.math.BigDecimal;

public record AiHarvestEvent(
        Long cultivationId,
        Long userId,
        String cultivationName,
        BigDecimal harvestWeight
) {
}
