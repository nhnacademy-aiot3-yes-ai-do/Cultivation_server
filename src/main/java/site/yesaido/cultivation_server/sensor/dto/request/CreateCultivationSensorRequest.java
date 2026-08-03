package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.util.List;

public record CreateCultivationSensorRequest(
        @NotBlank
        @Size(max = 32, message = "센서 고유번호는 32자 이하여야 합니다.")
        String deviceEui,

        @NotBlank
        @Size(max = 100, message = "센서 모델명은 100자 이하여야합니다.")
        String deviceModel,

        @NotBlank
        @Size(max = 100, message = "센서 이름은 100자 이하여야합니다.")
        String deviceName,

        @Size(max = 10, message = "센서 위치는 10자이어야합니다.")
        String location,

        @Size(max = 100, message = "센서 상세 위치는 100자 이하여야합니다.")
        String locationDetail,

        @NotEmpty
        List<@Valid SensorSettingRequest> sensorSettings
) {
    public CultivationSensor toEntity (long cultivationId) {
        return new CultivationSensor(cultivationId, deviceEui, deviceModel, deviceName, location, locationDetail);
    }

}