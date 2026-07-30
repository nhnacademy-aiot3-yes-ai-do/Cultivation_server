package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.ConflictException;

public class CultivationMemberAlreadyExistException extends ConflictException {
    public CultivationMemberAlreadyExistException(Long userId) {
        super("이미 해당 재배의 멤버입니다: " + userId);
    }
}
