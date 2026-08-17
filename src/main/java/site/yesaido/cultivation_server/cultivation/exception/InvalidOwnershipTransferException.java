package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidOwnershipTransferException extends BadRequestException {
    public InvalidOwnershipTransferException() {
        super("자기 자신에게는 소유권을 이전할 수 없습니다.");
    }
}
