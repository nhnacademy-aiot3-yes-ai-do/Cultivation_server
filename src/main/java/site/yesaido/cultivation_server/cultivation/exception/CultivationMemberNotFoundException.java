package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.NotFoundException;

public class CultivationMemberNotFoundException extends NotFoundException {
    public CultivationMemberNotFoundException() {
        super("해당 경작의 멤버를 찾을 수 없습니다.");
    }
}
