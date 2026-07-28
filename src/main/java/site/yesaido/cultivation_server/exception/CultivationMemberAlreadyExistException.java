package site.yesaido.cultivation_server.exception;

public class CultivationMemberAlreadyExistException extends RuntimeException {
    public CultivationMemberAlreadyExistException(Long userId) {
        super("이미 해당 재배의 멤버입니다: " + userId);
    }
}
