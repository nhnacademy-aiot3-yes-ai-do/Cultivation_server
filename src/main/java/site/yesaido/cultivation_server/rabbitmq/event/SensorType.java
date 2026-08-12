package site.yesaido.cultivation_server.rabbitmq.event;

import java.util.Locale;

public enum SensorType {
    TEMPERATURE,
    HUMIDITY,
    CO2,
    LIGHT;

    public static SensorType fromString(String type) {
        try {
            return SensorType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}