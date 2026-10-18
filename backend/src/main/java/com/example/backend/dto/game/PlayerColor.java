package com.example.backend.dto.game;

import com.example.backend.game.Color;

/** Цвет игрока, выбираемый при старте партии (ТЗ 5.1). ИИ играет противоположным. */
public enum PlayerColor {
    WHITE,
    BLACK;

    public Color toColor() {
        return this == WHITE ? Color.WHITE : Color.BLACK;
    }

    public static PlayerColor of(Color color) {
        return color == Color.WHITE ? WHITE : BLACK;
    }
}
