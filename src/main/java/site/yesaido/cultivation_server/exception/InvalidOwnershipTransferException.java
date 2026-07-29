package site.yesaido.cultivation_server.exception;

public class InvalidOwnershipTransferException extends RuntimeException {
    public InvalidOwnershipTransferException() {
        super("자기 자신에게는 소유권을 이전할 수 없습니다.");
    }
}
