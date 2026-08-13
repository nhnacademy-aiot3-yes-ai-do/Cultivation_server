package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class MushroomReferenceNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "[mushroom-reference] 해당하는 버섯참를 찾지 못했습니다";
    public MushroomReferenceNotFoundException(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
