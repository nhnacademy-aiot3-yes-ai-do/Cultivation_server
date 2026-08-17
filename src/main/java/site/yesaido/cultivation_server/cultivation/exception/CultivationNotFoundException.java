package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class CultivationNotFoundException extends NotFoundException {
    public CultivationNotFoundException(Long cultivationId) {
        super("존재하지 않는 경작입니다: " + cultivationId);
    }
}
