package com.example.backend.error;

import com.example.backend.game.InvalidMoveException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Единый формат ошибок API (ТЗ 5.3): любое исключение превращается
 * в {@link ApiErrorResponse} с кодом из {@link ApiErrorCode}.
 * <p>Неожиданные сбои логируются и отдаются как {@code INTERNAL_ERROR} (500).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException exception,
                                                      HttpServletRequest request) {
        return build(exception.code(), exception.getMessage(), request);
    }

    /** Jakarta Validation на теле запроса: {@code VALIDATION_ERROR} со списком полей. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                             HttpServletRequest request) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::describeField)
                .collect(Collectors.joining("; "));
        return build(ApiErrorCode.VALIDATION_ERROR,
                details.isEmpty() ? "Некорректные данные запроса" : details, request);
    }

    /** Некорректный JSON или значение enum: {@code VALIDATION_ERROR}. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception,
                                                             HttpServletRequest request) {
        return build(ApiErrorCode.VALIDATION_ERROR,
                "Некорректное тело запроса: " + exception.getMostSpecificCause().getMessage(),
                request);
    }

    /** Несовпадающий тип параметра пути (например, id не UUID): {@code VALIDATION_ERROR}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                               HttpServletRequest request) {
        return build(ApiErrorCode.VALIDATION_ERROR,
                "Некорректное значение параметра: " + exception.getName(), request);
    }

    /** Страховка: движок отверг ход — отдаём {@code INVALID_MOVE}. */
    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMove(InvalidMoveException exception,
                                                              HttpServletRequest request) {
        return build(ApiErrorCode.INVALID_MOVE, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception,
                                                             HttpServletRequest request) {
        log.error("Непредвиденная ошибка обработки запроса {}", request.getRequestURI(), exception);
        return build(ApiErrorCode.INTERNAL_ERROR, "Внутренняя ошибка сервера", request);
    }

    private String describeField(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> build(ApiErrorCode code, String message,
                                                   HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), code.status().value(), code.name(), message, request.getRequestURI());
        return ResponseEntity.status(code.status()).body(body);
    }
}
