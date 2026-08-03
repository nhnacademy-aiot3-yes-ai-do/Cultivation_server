package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.cultivation_server.exception.client.BadRequestException;

public class MushroomReferenceThresholdBadRequest extends BadRequestException {
    private static final String DEFAULT_MESSAGE = "[mushroom-reference-threshold] 해당하는 버섯 임계값 요청이 잘못되었습니다";
    public MushroomReferenceThresholdBadRequest(String content) {
        super(DEFAULT_MESSAGE, "%s - %s".formatted(DEFAULT_MESSAGE, content));
    }
}
