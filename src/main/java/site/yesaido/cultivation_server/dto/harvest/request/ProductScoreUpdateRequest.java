package site.yesaido.cultivation_server.dto.harvest.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductScoreUpdateRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")BigDecimal productScore
        ) {
}
