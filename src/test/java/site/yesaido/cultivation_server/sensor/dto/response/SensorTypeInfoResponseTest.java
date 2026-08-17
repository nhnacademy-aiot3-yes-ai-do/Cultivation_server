package site.yesaido.cultivation_server.sensor.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import static org.assertj.core.api.Assertions.assertThat;

class SensorTypeInfoResponseTest {

    @Test
    @DisplayName("from() - SensorType을 id/type/valueUnit으로 변환한다")
    void fromMapsSensorType() {
        SensorType sensorType = new SensorType("HUMIDITY", "%");
        ReflectionTestUtils.setField(sensorType, "id", 1L);

        SensorTypeInfoResponse response = SensorTypeInfoResponse.from(sensorType);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo("HUMIDITY");
        assertThat(response.valueUnit()).isEqualTo("%");
    }
}