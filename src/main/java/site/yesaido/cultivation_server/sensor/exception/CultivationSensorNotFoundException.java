package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class CultivationSensorNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "[CultivationSensor] 해당 경작지에 존재하지 않는 센서 요청입니다";

    public CultivationSensorNotFoundException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
