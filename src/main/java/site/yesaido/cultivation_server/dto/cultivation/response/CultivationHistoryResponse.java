package site.yesaido.cultivation_server.dto.cultivation.response;

import site.yesaido.cultivation_server.entity.cultivation.CultivationStatus;
import site.yesaido.cultivation_server.entity.harvest.ProductGrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CultivationHistoryResponse(
        Long cultivationId,
        String name,
        Long mushroomId,
        CultivationStatus status,
        BigDecimal harvestWeight,
        ProductGrade productGrade,
        LocalDateTime finishedAt
) {}
