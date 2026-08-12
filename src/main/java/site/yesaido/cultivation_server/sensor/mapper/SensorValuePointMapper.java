package site.yesaido.cultivation_server.sensor.mapper;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import site.yesaido.cultivation_server.rabbitmq.event.SensorValueEvent;

import java.time.ZoneOffset;
import java.util.Objects;

public class SensorValuePointMapper {

    public static final String MEASUREMENT = "sensor_value";
    public static final String VALUE_FIELD = "value";

    public Point toPoint(SensorValueEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.value(), "event.value must not be null");
        Objects.requireNonNull(event.time(), "event.time must not be null");
        Objects.requireNonNull(event.cultivationId(), "event.cultivationId must not be null");
        requireText(event.deviceEui(), "event.deviceEui must not be blank");
        Objects.requireNonNull(event.sensorType(), "event.sensorType must not be null");
        Objects.requireNonNull(event.unit(), "event.unit must not be null");

        Point point = Point.measurement(MEASUREMENT)
                .addField(VALUE_FIELD, event.value())
                .time(event.time().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addTag(point, "place", event.place());
        addTag(point, "location", event.location());
        addTag(point, "deviceModel", event.deviceModel());
        addTag(point, "deviceName", event.deviceName());
        addTag(point, "deviceEui", event.deviceEui());
        addTag(point, "sensorType", event.sensorType());
        addTag(point, "unit", event.unit());
        addTag(point, "cultivationId", event.cultivationId().toString());

        return point;
    }

    private void addTag(Point point, String key, String value) {
        if (value != null && !value.isBlank()) {
            point.addTag(key, value);
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
