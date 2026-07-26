package site.yesaido.cultivation_server.exception;

public class CultivationAlreadyExist extends RuntimeException {
    public CultivationAlreadyExist(String name) {
        super("이미 존재하는 이름 입니다. : " + name);
    }
}
