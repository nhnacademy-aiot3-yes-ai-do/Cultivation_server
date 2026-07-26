package site.yesaido.cultivation_server.dto.sensor.response;


public record RegisteredSensor(
        String deviceEui,
        Long sensorId
) {}
