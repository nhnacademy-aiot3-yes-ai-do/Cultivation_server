package site.yesaido.cultivation_server.sensor.exception;

import site.yesaido.common.exception.server.CustomServerException;
import site.yesaido.common.exception.server.ServerErrorLevel;

public class InvalidSensorRangeEventException extends CustomServerException {
    private static final String DEFAULT_MESSAGE =
            "센서 범위 이벤트 생성 중 오류가 발생했습니다.";

    public InvalidSensorRangeEventException(String logContent) {
        super(
                DEFAULT_MESSAGE,
                "[SensorRangeEvent] " + logContent,
                ServerErrorLevel.ERROR_LEVEL
        );
    }
}
