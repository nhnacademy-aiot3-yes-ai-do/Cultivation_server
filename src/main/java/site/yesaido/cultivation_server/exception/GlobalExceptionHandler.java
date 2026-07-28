package site.yesaido.cultivation_server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.yesaido.cultivation_server.dto.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 400 BAD_REQUEST
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(InvalidMemberRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMemberRoleException(InvalidMemberRoleException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    // 403 FORBIDDEN
    @ExceptionHandler(CultivationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCultivationAccessDenied(CultivationAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    //404 NOT_FOUND
    @ExceptionHandler(CultivationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCultivationNotFound(CultivationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(MushroomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMushroomNotFoundException(MushroomNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(CultivationMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCultivationMemberNotFound(CultivationMemberNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    // 409 CONFLICT
    @ExceptionHandler(CultivationAlreadyExist.class)
    public ResponseEntity<ErrorResponse> handleCultivationAlreadyExist(CultivationAlreadyExist e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    @ExceptionHandler(CultivationAlreadyFinishedException.class)
    public ResponseEntity<ErrorResponse> handleCultivationAlreadyFinished(CultivationAlreadyFinishedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    //500 INTERNAL_SERVER_ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류 발생함."));
    }
}
