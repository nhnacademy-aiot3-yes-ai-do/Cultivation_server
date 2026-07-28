package site.yesaido.cultivation_server.exception;

public class CultivationMemberNotFoundException extends RuntimeException {
    public CultivationMemberNotFoundException() {
        super("해당 경작의 멤버를 찾을 수 없습니다.");
    }
}
