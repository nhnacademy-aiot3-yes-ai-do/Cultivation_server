package site.yesaido.cultivation_server.sensor.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCultivationSensorRequestTest {

    @Test
    @DisplayName("toEntity() - 요청 필드로 CultivationSensor 엔티티를 생성한다")
    void toEntityBuildsCultivationSensor() {
        CreateCultivationSensorRequest request = new CreateCultivationSensorRequest(
                "EUI-001",
                "MODEL-A",
                "배양실 센서",
                "ROOM-1",
                "북쪽 선반",
                List.of(new EnvironmentSettingRequest(1L, new BigDecimal("18.0"), new BigDecimal("24.0")))
        );

        CultivationSensor sensor = request.toEntity(10L);

        assertThat(sensor.getCultivationId()).isEqualTo(10L);
        assertThat(sensor.getDeviceEui()).isEqualTo("EUI-001");
        assertThat(sensor.getDeviceModel()).isEqualTo("MODEL-A");
        assertThat(sensor.getDeviceName()).isEqualTo("배양실 센서");
        assertThat(sensor.getLocation()).isEqualTo("ROOM-1");
        assertThat(sensor.getLocationDetail()).isEqualTo("북쪽 선반");
    }
}