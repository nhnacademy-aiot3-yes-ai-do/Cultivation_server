package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.NotFoundException;

public class MushroomNotFoundException extends NotFoundException {
    public MushroomNotFoundException(Long mushroomId) {
        super("존재하지 않는 버섯 항목입니다. mushroomId=" + mushroomId);
    }
}
