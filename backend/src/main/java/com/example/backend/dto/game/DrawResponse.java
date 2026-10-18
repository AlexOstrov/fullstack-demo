package com.example.backend.dto.game;

/**
 * Ответ на предложение ничьей (ТЗ 5): решает ИИ.
 * {@code accepted=false} — предложение отклонено, партия продолжается.
 */
public record DrawResponse(boolean accepted, GameStateResponse game) {
}
