package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.ConflictException;

public class CultivationNotInHarvestModeException extends ConflictException {
    public CultivationNotInHarvestModeException(Long cultivationId) {
        super("수확 모드로 전환한 뒤에만 수확을 등록할 수 있습니다: " + cultivationId);
    }
}