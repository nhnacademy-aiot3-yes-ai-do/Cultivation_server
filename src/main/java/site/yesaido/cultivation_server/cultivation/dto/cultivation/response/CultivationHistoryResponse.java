package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import site.yesaido.cultivation_server.cultivation.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.cultivation.entity.harvest.ProductGrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CultivationHistoryResponse(
        Long cultivationId,
        String name,
        Long mushroomId,
        CultivationStatus status,
        BigDecimal harvestWeight,
        BigDecimal productScore,
        ProductGrade productGrade,
        LocalDateTime finishedAt
) {}
