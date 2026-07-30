package site.yesaido.cultivation_server.cultivation.dto.environmentsetting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RecommendedEnvironment(
        @Valid @NotNull EnvironmentRange temperature,
        @Valid @NotNull EnvironmentRange humidity,
        @Valid @NotNull EnvironmentRange co2,
        @Valid @NotNull EnvironmentRange light
) {}
