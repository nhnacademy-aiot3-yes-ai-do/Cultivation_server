package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class DuplicateMushroomReferenceThresholdException extends BadRequestException {
    public DuplicateMushroomReferenceThresholdException(Long sensorTypeId, String thresholdType) {
        super("중복된 버섯 참조 임계값입니다. sensorTypeId:%d, thresholdType:%s".formatted(sensorTypeId, thresholdType));
    }
}
