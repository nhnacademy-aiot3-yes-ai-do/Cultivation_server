package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidThresholdRangeException extends BadRequestException {
    private static final String DEFAULT_MESSAGE = "[EnvironmentSetting] 유효하지않는 경계값 범위입니다!";

    public InvalidThresholdRangeException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
