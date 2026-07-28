package site.yesaido.cultivation_server.exception;

public class CultivationAlreadyFinishedException extends RuntimeException {
    public CultivationAlreadyFinishedException(Long cultivationId) {
        super("이미 종료된 재배입니다: " + cultivationId);
    }
}
