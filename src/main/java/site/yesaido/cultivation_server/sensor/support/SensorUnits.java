package site.yesaido.cultivation_server.sensor.support;

public final class SensorUnits {

    public static final String CELSIUS = "°C";
    public static final String FAHRENHEIT = "°F";

    private SensorUnits() {
    }

    public static String normalize(String unit) {
        if (unit == null) {
            return null;
        }

        String stripped = unit.strip();

        return switch (stripped) {
            case "℃" -> CELSIUS;
            default -> stripped;
        };
    }
}