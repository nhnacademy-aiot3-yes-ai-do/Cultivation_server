package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.cultivation_server.exception.client.ConflictException;

public class MushroomReferenceAlreadyExistException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "[mushroom-reference] 이미 존재하는 버섯참조 생성 요청입니다";
    public MushroomReferenceAlreadyExistException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
