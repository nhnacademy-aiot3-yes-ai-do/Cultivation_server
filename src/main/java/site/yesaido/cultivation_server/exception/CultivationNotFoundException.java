package site.yesaido.cultivation_server.exception;

public class CultivationNotFoundException extends RuntimeException {
    public CultivationNotFoundException(Long cultivationId) {
        super("존재하지 않는 경작입니다: " + cultivationId);
    }
}
