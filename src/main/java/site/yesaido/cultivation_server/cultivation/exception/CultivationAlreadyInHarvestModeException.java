package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationAlreadyInHarvestModeException extends ConflictException {
    public CultivationAlreadyInHarvestModeException(Long cultivationId) {
        super("이미 수확 모드로 전환된 재배입니다: " + cultivationId);
    }
}