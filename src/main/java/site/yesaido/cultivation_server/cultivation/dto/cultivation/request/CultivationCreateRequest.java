package site.yesaido.cultivation_server.cultivation.dto.cultivation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import site.yesaido.cultivation_server.cultivation.dto.sensor.request.DeviceRegisterRequest;

import java.util.List;

public record CultivationCreateRequest(
        @NotBlank(message = "경작 이름을 입력해주세요.")
        @Size(max = 100)
        String name,

        @NotNull(message = "버섯 ID는 필수입니다.")
        Long mushroomId,

        @Valid List<DeviceRegisterRequest> devices
) {}
