package site.yesaido.cultivation_server.cultivation.dto.harvest.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record HarvestCreateRequest(
        @NotNull @DecimalMin("0.0") BigDecimal harvestWeight,
        String memo
        ) {
}
