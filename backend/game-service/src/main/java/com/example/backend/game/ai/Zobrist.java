package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.Piece;

import java.util.Random;

/**
 * Zobrist-хэширование позиций для транспозиционной таблицы (ТЗ 6):
 * 64-битные случайные ключи на каждую (фигура, клетка) и на очередь хода.
 * Таблица ключей детерминирована фиксированным seed — результаты воспроизводимы.
 */
public final class Zobrist {

    /** Фиксированный seed: ключи одинаковы во всех запусках. */
    private static final long SEED = 0x5EED_C0DE_5EED_C0DEL;

    /** Единственный экземпляр — таблица ключей неизменна. */
    public static final Zobrist INSTANCE = new Zobrist();

    // индексы фигур: ordinal() - 1 (EMPTY не занимает ключ)
    private final long[][] pieceKeys = new long[4][Board.SIZE * Board.SIZE];
    private final long sideToMoveKey;

    public Zobrist() {
        Random random = new Random(SEED);
        for (long[] keys : pieceKeys) {
            for (int i = 0; i < keys.length; i++) {
                keys[i] = random.nextLong();
            }
        }
        sideToMoveKey = random.nextLong();
    }

    /** Хэш расстановки и очереди хода. */
    public long hash(Board board, Color sideToMove) {
        long hash = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (!piece.isEmpty()) {
                    hash ^= pieceKeys[piece.ordinal() - 1][row * Board.SIZE + col];
                }
            }
        }
        if (sideToMove == Color.BLACK) {
            hash ^= sideToMoveKey;
        }
        return hash;
    }
}
