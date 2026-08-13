package site.yesaido.cultivation_server.sensor.service.influx;

import org.junit.jupiter.api.Test;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;
import site.yesaido.cultivation_server.sensor.mapper.SensorValuePointMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorValuePointMapperTest {

    private final SensorValuePointMapper mapper = new SensorValuePointMapper();

    @Test
    void mapsSensorEventToInfluxPoint() {
        OffsetDateTime time = OffsetDateTime.of(2026, 8, 9, 12, 34, 56, 0, ZoneOffset.UTC);
        SensorValueEvent event = new SensorValueEvent(
                "farm-a",
                "room-1",
                "model-x",
                "sensor-01",
                "eui-01",
                "TEMPERATURE",
                "°C",
                BigDecimal.valueOf(23.5),
                time,
                42L
        );

        var point = mapper.toPoint(event);

        assertThat(point.toLineProtocol())
                .contains("sensor_value")
                .contains("place=farm-a")
                .contains("location=room-1")
                .contains("deviceModel=model-x")
                .contains("deviceName=sensor-01")
                .contains("deviceEui=eui-01")
                .contains("sensorType=TEMPERATURE")
                .contains("unit=°C")
                .contains("cultivationId=42")
                .contains("value=23.5");
        assertThat(point.getTime()).isNotNull();
    }

    @Test
    void rejectsEventWithoutCultivationId() {
        SensorValueEvent event = event(null, "eui-01", "TEMPERATURE");

        assertThatThrownBy(() -> mapper.toPoint(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("event.cultivationId must not be null");
    }

    @Test
    void rejectsEventWithoutDeviceEui() {
        SensorValueEvent event = event(42L, " ", "TEMPERATURE");

        assertThatThrownBy(() -> mapper.toPoint(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event.deviceEui must not be blank");
    }

    @Test
    void rejectsEventWithoutSensorType() {
        SensorValueEvent event = event(42L, "eui-01", null);

        assertThatThrownBy(() -> mapper.toPoint(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event.sensorType must not be null");
    }

    private SensorValueEvent event(Long cultivationId, String deviceEui, String sensorType) {
        return new SensorValueEvent(
                "farm-a",
                "room-1",
                "model-x",
                "sensor-01",
                deviceEui,
                sensorType,
                "°C",
                BigDecimal.valueOf(23.5),
                OffsetDateTime.of(2026, 8, 9, 12, 34, 56, 0, ZoneOffset.UTC),
                cultivationId
        );
    }
}
