package site.yesaido.cultivation_server.exception;

public class CultivationAccessDeniedException extends RuntimeException {
    public CultivationAccessDeniedException(Long cultivationId) {
        super("접근 권한이 없는 재배입니다: " + cultivationId);
    }
}
