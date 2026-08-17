package site.yesaido.cultivation_server.sensor.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.entity.EnvironmentSetting;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSettingResponseTest {

    @Test
    @DisplayName("from() - EnvironmentSetting을 응답 필드로 변환한다")
    void fromMapsEnvironmentSetting() {
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        ReflectionTestUtils.setField(sensorType, "id", 3L);
        EnvironmentSetting setting = new EnvironmentSetting(
                10L, sensorType, new BigDecimal("18.0"), new BigDecimal("24.0"));

        EnvironmentSettingResponse response = EnvironmentSettingResponse.from(setting);

        assertThat(response.sensorTypeId()).isEqualTo(3L);
        assertThat(response.thresholdMin()).isEqualByComparingTo("18.0");
        assertThat(response.thresholdMax()).isEqualByComparingTo("24.0");
    }
}