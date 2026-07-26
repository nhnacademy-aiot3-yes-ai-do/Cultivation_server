package site.yesaido.cultivation_server.dto.environmentsetting;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EnvironmentRange(
        @NotNull(message = "최소값은 필수입니다.")
        BigDecimal min,
        @NotNull(message = "최대값은 필수입니다.")
        BigDecimal max
) {}
