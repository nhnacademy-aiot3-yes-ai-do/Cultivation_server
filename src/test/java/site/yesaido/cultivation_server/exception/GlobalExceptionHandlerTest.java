package site.yesaido.cultivation_server.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.yesaido.cultivation_server.exception.client.*;
import site.yesaido.cultivation_server.exception.server.CustomServerException;
import site.yesaido.cultivation_server.exception.server.ServerErrorLevel;

import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("Bad Request Exception")
        void handleBadRequestException() {
            String message = "test-message";
            BadRequestException exception = new BadRequestException(message);

            ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }

        @Test
        @DisplayName("Method Argument Not Valid Exception")
        void handleMethodArgumentNotValidException() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("objectName", "field", "test-message");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                    () -> Assertions.assertEquals("test-message", Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }

        @Test
        @DisplayName("Missing Request Header Exception")
        void handleMissingRequestHeaderException() {
            MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
            when(exception.getHeaderName()).thenReturn("X-Test-Header");

            ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeaderException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                    () -> Assertions.assertEquals(
                            "필수 헤더가 누락되었습니다: X-Test-Header",
                            Objects.requireNonNull(response.getBody()).getBody().getDetail()
                    )
            );
        }

        @Test
        @DisplayName("Max Upload Size Exceeded Exception")
        void handleMaxUploadSizeExceededException() {
            MaxUploadSizeExceededException exception = mock(MaxUploadSizeExceededException.class);

            ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceededException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                    () -> Assertions.assertEquals(
                            "사진 파일 크기는 10MB를 초과할 수 없습니다.",
                            Objects.requireNonNull(response.getBody()).getBody().getDetail()
                    )
            );
        }
    }

    @Nested
    @DisplayName("401 Unauthorized")
    class Unauthorized {

        @Test
        @DisplayName("Unauthorized Exception")
        void handleUnauthorizedException() {
            String message = "test-message";
            UnauthorizedException exception = new UnauthorizedException(message);

            ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Nested
    @DisplayName("403 Forbidden")
    class Forbidden {

        @Test
        @DisplayName("Forbidden Exception")
        void handleForbiddenException() {
            String message = "test-message";
            ForbiddenException exception = new ForbiddenException(message);

            ResponseEntity<ErrorResponse> response = handler.handleForbiddenExceptionException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Nested
    @DisplayName("404 Not Found")
    class NotFound {

        @Test
        @DisplayName("Not Found Exception")
        void handleNotFoundException() {
            String message = "test-message";
            NotFoundException exception = new NotFoundException(message);

            ResponseEntity<ErrorResponse> response = handler.handleNotFoundExceptionException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Nested
    @DisplayName("409 Conflict")
    class Conflict {

        @Test
        @DisplayName("Conflict Exception")
        void handleConflictException() {
            String message = "test-message";
            ConflictException exception = new ConflictException(message);

            ResponseEntity<ErrorResponse> response = handler.handleConflictException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Nested
    @DisplayName("415 Unsupported Media Type")
    class UnsupportedMediaType {

        @Test
        @DisplayName("Unsupported Media Type Exception")
        void handleUnsupportedMediaTypeException() {
            String message = "test-message";
            UnsupportedMediaTypeException exception = new UnsupportedMediaTypeException(message);

            ResponseEntity<ErrorResponse> response = handler.handleUnsupportedMediaTypeException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Nested
    @DisplayName("500 Custom Server")
    class CustomServer {

        @Test
        @DisplayName("Custom Server Exception - WARN")
        void handleCustomServerExceptionWarn() {
            String message = "test-message";
            ServerErrorLevel level = ServerErrorLevel.WARN_LEVEL;
            CustomServerException exception = new CustomServerException(message, level);

            ResponseEntity<ErrorResponse> response = handler.handleServerException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }

        @Test
        @DisplayName("Custom Server Exception - ERROR")
        void handleCustomServerExceptionError() {
            String message = "test-message";
            ServerErrorLevel level = ServerErrorLevel.ERROR_LEVEL;
            CustomServerException exception = new CustomServerException(message, level);

            ResponseEntity<ErrorResponse> response = handler.handleServerException(exception);

            Assertions.assertAll(
                    () -> Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()),
                    () -> Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail())
            );
        }
    }

    @Test
    @DisplayName("500 Server Exception")
    void handleServerException() {
        String message = "test-message";
        RuntimeException exception = new RuntimeException(message);

        ResponseEntity<ErrorResponse> response = handler.handleException(exception);

        Assertions.assertEquals(message, Objects.requireNonNull(response.getBody()).getBody().getDetail());
    }
}
