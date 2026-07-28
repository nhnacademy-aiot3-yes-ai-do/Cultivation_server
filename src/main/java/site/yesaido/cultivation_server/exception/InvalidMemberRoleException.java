package site.yesaido.cultivation_server.exception;

public class InvalidMemberRoleException extends RuntimeException {
    public InvalidMemberRoleException() {
        super("OWNER 권한은 이 API로 부여할 수 없습니다.");
    }
}
