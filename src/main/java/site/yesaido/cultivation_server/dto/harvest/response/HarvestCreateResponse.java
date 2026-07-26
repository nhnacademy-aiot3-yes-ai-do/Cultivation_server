package site.yesaido.cultivation_server.dto.harvest.response;

import site.yesaido.cultivation_server.entity.harvest.ProductGrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HarvestCreateResponse(
        Long harvestId,
        BigDecimal harvestWeight,
        LocalDateTime harvestedAt,
        BigDecimal productScore,
        ProductGrade productGrade
) {
}
