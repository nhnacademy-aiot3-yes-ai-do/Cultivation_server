package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.BadRequestException;

public class InvalidMemberRoleException extends BadRequestException {
    public InvalidMemberRoleException() {
        super("OWNER 권한은 이 API로 부여할 수 없습니다.");
    }
}
