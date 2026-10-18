package com.example.backend.error;

import org.springframework.http.HttpStatus;

/**
 * Коды ошибок API (ТЗ 5.3) и соответствующие HTTP-статусы.
 * {@code AI_BUSY}/{@code STALE_STATE} — 409 Conflict: запрос конфликтует
 * с текущим состоянием партии и может быть повторён.
 */
public enum ApiErrorCode {
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_YOUR_TURN(HttpStatus.BAD_REQUEST),
    INVALID_MOVE(HttpStatus.BAD_REQUEST),
    STALE_STATE(HttpStatus.CONFLICT),
    GAME_FINISHED(HttpStatus.BAD_REQUEST),
    AI_BUSY(HttpStatus.CONFLICT),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
