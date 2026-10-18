package com.example.backend.game;

/**
 * Недопустимый ход (ТЗ 4.4): {@code message} — человекочитаемая причина отказа,
 * API-слой возвращает её в ответе ошибки.
 */
public class InvalidMoveException extends RuntimeException {

    public InvalidMoveException(String reason) {
        super(reason);
    }
}
