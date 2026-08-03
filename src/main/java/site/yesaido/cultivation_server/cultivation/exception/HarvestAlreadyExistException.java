package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.ConflictException;

public class HarvestAlreadyExistException extends ConflictException {
    public HarvestAlreadyExistException(Long cultivationId) {
        super("이미 종료 기록된 재배입니다: " + cultivationId);
    }
}
