package site.yesaido.cultivation_server.exception;

public class MushroomNotFoundException extends RuntimeException {
    public MushroomNotFoundException(Long mushroomId) {
        super("존재하지 않는 버섯 항목입니다. mushroomId=" + mushroomId);
    }
}
