package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.ForbiddenException;

public class CultivationAccessDeniedException extends ForbiddenException {
    public CultivationAccessDeniedException(Long cultivationId) {
        super("접근 권한이 없는 재배입니다: " + cultivationId);
    }
}
