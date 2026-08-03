package site.yesaido.cultivation_server.cultivation.exception;

import site.yesaido.cultivation_server.exception.client.NotFoundException;

public class PhotoNotFoundException extends NotFoundException {
    public PhotoNotFoundException(Long photoId) {
        super("존재하지 않는 사진입니다: " + photoId);
    }
}
