package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationSensorAlreadyExistException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "이미 등록되어 사용 중인 센서 고유번호입니다. 기존 등록을 해제하거나 다른 기기를 선택해 주세요.";

    public CultivationSensorAlreadyExistException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
