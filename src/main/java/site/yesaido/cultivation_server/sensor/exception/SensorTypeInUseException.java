package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.client.ConflictException;

public class SensorTypeInUseException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "[sensor-type] 다른 데이터에서 참조 중인 센서 타입은 삭제할 수 없습니다";

    public SensorTypeInUseException(long sensorTypeId) {
        super(DEFAULT_MESSAGE, "%s - sensorTypeId:%d".formatted(DEFAULT_MESSAGE, sensorTypeId));
    }
}
