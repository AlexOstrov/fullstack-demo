package com.example.backend.error;

/**
 * Ошибка API с кодом (ТЗ 5.3): обрабатывается {@link GlobalExceptionHandler}
 * и превращается в единый формат ответа об ошибке.
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;

    public ApiException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiErrorCode code() {
        return code;
    }
}
