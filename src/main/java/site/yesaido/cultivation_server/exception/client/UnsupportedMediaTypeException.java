package site.yesaido.cultivation_server.exception.client;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnsupportedMediaTypeException extends RuntimeException {
    @Getter
    private static final HttpStatus code = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    private final String logContent;
    public UnsupportedMediaTypeException(String message) {
        this(message, message);
    }
    public UnsupportedMediaTypeException(String message, String logContent) {
        super(message);
        this.logContent = logContent;
    }
}
