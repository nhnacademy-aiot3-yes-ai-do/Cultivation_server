package site.yesaido.cultivation_server.sensor.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.validation.UniqueSensorTypeIds;

import java.util.List;

public record CreateCultivationSensorRequest(
        @NotBlank
        @Size(max = 32, message = "센서 고유번호는 32자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^#+]*$",
                message = "센서 고유번호에는 #, + 문자를 사용할 수 없습니다."
        )
        String deviceEui,

        @NotBlank
        @Size(max = 100, message = "센서 모델명은 100자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^#+]*$",
                message = "센서 모델명에는 #, + 문자를 사용할 수 없습니다."
        )
        String deviceModel,

        @NotBlank
        @Size(max = 100, message = "센서 이름은 100자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^#+]*$",
                message = "센서 이름에는 #, + 문자를 사용할 수 없습니다."
        )
        String deviceName,

        @NotBlank
        @Size(max = 10, message = "센서 위치는 10자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^#+]*$",
                message = "센서 위치에는 #, + 문자를 사용할 수 없습니다."
        )
        String location,

        @NotBlank
        @Size(max = 100, message = "센서 상세 위치는 100자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^#+]*$",
                message = "센서 상세 위치에는 #, + 문자를 사용할 수 없습니다."
        )
        String locationDetail,

        @UniqueSensorTypeIds // 커스텀 어노테이션 (센서 세팅 요청의 중복 센서 id 존재여부 검사)
        @NotEmpty(message = "센서 설정은 하나 이상 필요합니다.")
        List<@NotNull @Valid SensorSettingRequest> sensorSettings
) {
    public CultivationSensor toEntity (long cultivationId) {
        return new CultivationSensor(cultivationId, deviceEui, deviceModel, deviceName, location, locationDetail);
    }

}