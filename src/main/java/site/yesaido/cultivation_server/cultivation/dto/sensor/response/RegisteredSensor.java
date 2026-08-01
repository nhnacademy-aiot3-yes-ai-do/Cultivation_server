package site.yesaido.cultivation_server.cultivation.dto.sensor.response;


public record RegisteredSensor(
        String deviceEui,
        Long sensorId
) {}
