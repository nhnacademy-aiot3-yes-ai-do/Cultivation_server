package site.yesaido.cultivation_server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.yesaido.cultivation_server.cultivation.dto.ErrorResponse;
import site.yesaido.cultivation_server.exception.client.*;
import site.yesaido.cultivation_server.exception.server.CustomServerException;
import site.yesaido.cultivation_server.exception.server.ServerErrorLevel;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //400 Bad Request
    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    //401 Unauthorized
    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    //403 Forbidden
    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<ErrorResponse> handleForbiddenExceptionException(ForbiddenException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    //404 Not Found
    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundExceptionException(NotFoundException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    //409 Conflict
    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    //415 Unsupported Media Type
    @ExceptionHandler({UnsupportedMediaTypeException.class})
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaTypeException(BadRequestException e) {
        clientErrorPrint(e.getLogContent());
        return createResponseEntity(BadRequestException.getCode(), e.getMessage());
    }

    private void clientErrorPrint(String logContent) {
        log.info("{}", logContent);
    }

    //500 Custom Server Exception
    @ExceptionHandler({CustomServerException.class})
    public ResponseEntity<ErrorResponse> handleServerException(CustomServerException e) {
        if(e.getErrorLevel().equals(ServerErrorLevel.WARN_LEVEL)) {
            log.warn("{}", e.getLogContent());
        } else {
            log.error("{}", e.getLogContent());
        }

        return createResponseEntity(CustomServerException.getStatus(), e.getMessage());
    }

    //500 Server Exception
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.warn("{}", e.getMessage());
        return createResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<ErrorResponse> createResponseEntity(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message));
    }


    // Spring
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }
}
