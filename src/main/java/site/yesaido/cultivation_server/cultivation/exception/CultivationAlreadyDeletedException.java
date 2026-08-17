package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationAlreadyDeletedException extends ConflictException {
    public CultivationAlreadyDeletedException(Long cultivationId) {
        super("이미 삭제된 재배입니다: " + cultivationId);
    }
}
