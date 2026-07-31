package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.cultivation_server.exception.client.NotFoundException;

public class SensorTypeNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "[sensor-type] 해당하는 센서타입을 찾지 못했습니다";
    public SensorTypeNotFoundException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
