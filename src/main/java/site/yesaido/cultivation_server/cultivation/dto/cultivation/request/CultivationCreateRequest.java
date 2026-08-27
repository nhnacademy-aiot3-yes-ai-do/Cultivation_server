package site.yesaido.cultivation_server.cultivation.dto.cultivation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import site.yesaido.cultivation_server.sensor.dto.request.EnvironmentSettingRequest;
import site.yesaido.cultivation_server.sensor.validation.UniqueSensorTypeIds;

import java.util.List;

public record CultivationCreateRequest(
        @NotBlank(message = "경작 이름을 입력해주세요.")
        @Size(max = 100)
        String name,

        @NotNull(message = "버섯 ID는 필수입니다.")
        Long mushroomId,

        @NotEmpty(message = "환경 설정은 하나 이상 필요합니다.")
        @UniqueSensorTypeIds
        List<@NotNull @Valid EnvironmentSettingRequest> environmentSettingRequests
) {}
