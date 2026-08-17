package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationSensorAlreadyExistException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "[CultivationSensor] 해당 경작지에 이미 존재하는 센서 요청입니다";

    public CultivationSensorAlreadyExistException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
