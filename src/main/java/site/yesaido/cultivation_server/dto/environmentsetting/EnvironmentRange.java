package site.yesaido.cultivation_server.dto.environmentsetting;

import jakarta.validation.constraints.NotNull;
import site.yesaido.cultivation_server.util.ValidRange;

import java.math.BigDecimal;

@ValidRange
public record EnvironmentRange(
        @NotNull(message = "최소값은 필수입니다.")
        BigDecimal min,
        @NotNull(message = "최대값은 필수입니다.")
        BigDecimal max
) {}
