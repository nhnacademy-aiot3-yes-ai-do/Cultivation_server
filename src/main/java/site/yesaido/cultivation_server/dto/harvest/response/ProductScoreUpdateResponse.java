package site.yesaido.cultivation_server.dto.harvest.response;

import site.yesaido.cultivation_server.entity.harvest.ProductGrade;

import java.math.BigDecimal;

public record ProductScoreUpdateResponse(
        Long harvestId,
        BigDecimal productScore,
        ProductGrade productGrade
) {
}
