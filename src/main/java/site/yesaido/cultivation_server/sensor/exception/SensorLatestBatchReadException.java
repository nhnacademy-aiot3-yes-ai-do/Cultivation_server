package site.yesaido.cultivation_server.sensor.exception;

public class SensorLatestBatchReadException extends RuntimeException {
    public SensorLatestBatchReadException(Throwable cause) {
        super("사용자 센서 최신값 batch 조회에 실패했습니다.", cause);
    }
}
