package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.cultivation_server.exception.client.ConflictException;

public class SensorTypeAlreadyExistException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "[sensor-type] 이미 존재하는 센서타입 생성 요청입니다";
    public SensorTypeAlreadyExistException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}