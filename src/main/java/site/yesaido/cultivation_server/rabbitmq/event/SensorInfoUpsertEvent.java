package site.yesaido.cultivation_server.rabbitmq.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// rule engine으로 보낼때 사용
// 센서 추가시
// CRU
public record SensorInfoUpsertEvent(
        @NotNull
        Long cultivationId,

        @NotBlank
        String location,

        @NotBlank
        String locationDetail,

        @NotBlank
        String deviceModel,

        @NotBlank
        String deviceName,

        @NotBlank
        String deviceEui,

        @NotNull
        SensorType sensorType,

        @NotBlank
        String unit
) {
}
