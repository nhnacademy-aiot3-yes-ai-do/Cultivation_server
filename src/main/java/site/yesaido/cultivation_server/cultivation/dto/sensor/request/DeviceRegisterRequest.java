package site.yesaido.cultivation_server.cultivation.dto.sensor.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeviceRegisterRequest (
        @NotBlank(message = "Device EUI는 필수입니다.")
        @Size(max = 32)
        String deviceEui,
        @Size(max = 10)
        String location,
        @Size(max = 50)
        String locationDetail,
        @Size(max = 100)
        String deviceModel,
        @NotEmpty(message = "최소 1개 이상의 센서 타입 ID가 필요합니다.")
        List<Long> sensorTypeIds
) {}
