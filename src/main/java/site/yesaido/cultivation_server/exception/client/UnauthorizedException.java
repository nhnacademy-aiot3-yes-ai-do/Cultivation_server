package site.yesaido.cultivation_server.exception.client;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedException extends RuntimeException {
    @Getter
    private static final HttpStatus code = HttpStatus.UNAUTHORIZED;
    private final String logContent;
    public UnauthorizedException(String message) {
        this(message, message);
    }
    public UnauthorizedException(String message, String logContent) {
        super(message);
        this.logContent = logContent;
    }
}
