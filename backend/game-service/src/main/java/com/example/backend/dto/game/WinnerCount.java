package com.example.backend.dto.game;

/**
 * Проекция агрегата {@code tournament} по победителю (B1).
 * {@code winner}: {@code HUMAN}, {@code AI} или {@code DRAW}.
 */
public record WinnerCount(String winner, long total) {
}
