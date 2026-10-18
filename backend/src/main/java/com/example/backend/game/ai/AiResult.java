package com.example.backend.game.ai;

import com.example.backend.game.Move;

/**
 * Результат хода ИИ: выбранный ход (или {@code null}, если ходов нет),
 * завершённая итеративным углублением глубина, оценка лучшей линии
 * с точки зрения ходящей стороны, число посещённых узлов и затраченное время.
 */
public record AiResult(Move move, int depth, int score, long nodes, long elapsedMs) {

    public AiResult {
        if (depth < 0 || nodes < 0 || elapsedMs < 0) {
            throw new IllegalArgumentException("глубина, узлы и время неотрицательны");
        }
    }
}
