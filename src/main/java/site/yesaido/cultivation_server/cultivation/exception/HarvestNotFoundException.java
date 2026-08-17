package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class HarvestNotFoundException extends NotFoundException {
    public HarvestNotFoundException(Long cultivationId) {
        super("수확 기록이 존재하지 않는 재배입니다: " + cultivationId);
    }
}
