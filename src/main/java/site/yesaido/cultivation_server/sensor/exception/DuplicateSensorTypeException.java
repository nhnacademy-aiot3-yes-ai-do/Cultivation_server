package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.BadRequestException;

import java.util.Collection;

public class DuplicateSensorTypeException extends BadRequestException {
    private static final String DEFAULT_MESSAGE = "중복된 센서 타입이 존재합니다: ";

    public DuplicateSensorTypeException(Collection<Long> duplicateIds) {
        super(DEFAULT_MESSAGE + duplicateIds);
    }
}
