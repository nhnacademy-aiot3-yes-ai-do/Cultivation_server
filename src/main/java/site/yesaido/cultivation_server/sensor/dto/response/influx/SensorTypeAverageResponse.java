package site.yesaido.cultivation_server.sensor.dto.response.influx;

public record SensorTypeAverageResponse(
        Long cultivationId,
        String sensorType,
        String unit,
        Double averageValue
) {
}
