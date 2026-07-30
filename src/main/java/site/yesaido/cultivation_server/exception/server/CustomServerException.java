package site.yesaido.cultivation_server.exception.server;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomServerException extends RuntimeException {
    @Getter
    private static final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    private final String logContent;
    private final ServerErrorLevel errorLevel;
    public CustomServerException(String message, ServerErrorLevel errorLevel) {
        this(message, message, errorLevel);
    }
    public CustomServerException(String message, String logContent, ServerErrorLevel errorLevel) {
        super(message);
        this.logContent = logContent;
        this.errorLevel = errorLevel;
    }
}
