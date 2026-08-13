package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationAlreadyExist extends ConflictException {
    public CultivationAlreadyExist(String name) {
        super("이미 존재하는 이름 입니다. : " + name);
    }
}
