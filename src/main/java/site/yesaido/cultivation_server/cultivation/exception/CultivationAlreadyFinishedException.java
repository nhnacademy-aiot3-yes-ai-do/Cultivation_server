package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationAlreadyFinishedException extends ConflictException {
    public CultivationAlreadyFinishedException(Long cultivationId) {
        super("이미 종료된 재배입니다: " + cultivationId);
    }
}
