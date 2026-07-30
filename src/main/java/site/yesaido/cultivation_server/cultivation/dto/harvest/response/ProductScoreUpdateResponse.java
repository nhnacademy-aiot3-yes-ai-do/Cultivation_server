package site.yesaido.cultivation_server.cultivation.dto.harvest.response;

import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;

import java.math.BigDecimal;

public record ProductScoreUpdateResponse(
        Long harvestId,
        BigDecimal productScore,
        ProductGrade productGrade
) {
}
