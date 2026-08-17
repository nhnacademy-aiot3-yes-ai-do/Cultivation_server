package site.yesaido.cultivation_server.cultivation.dto.cultivation.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.sensor.dto.response.CultivationSensorTypeResponse;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensor;
import site.yesaido.cultivation_server.sensor.entity.CultivationSensorType;
import site.yesaido.cultivation_server.sensor.entity.SensorType;

import static org.assertj.core.api.Assertions.assertThat;

class CultivationSensorTypeResponseTest {

    @Test
    @DisplayName("from() - CultivationSensorType이 참조하는 SensorType 정보로 변환한다")
    void fromMapsSensorTypeFields() {
        CultivationSensor sensor = new CultivationSensor(
                10L, "EUI-001", "MODEL-A", "배양실 센서", "ROOM-1", "북쪽 선반");
        SensorType sensorType = new SensorType("TEMPERATURE", "C");
        CultivationSensorType relation = new CultivationSensorType(sensor, sensorType);

        CultivationSensorTypeResponse response = CultivationSensorTypeResponse.from(relation);

        assertThat(response.sensorTypeId()).isEqualTo(sensorType.getId());
        assertThat(response.type()).isEqualTo("TEMPERATURE");
        assertThat(response.valueUnit()).isEqualTo("C");
    }
}