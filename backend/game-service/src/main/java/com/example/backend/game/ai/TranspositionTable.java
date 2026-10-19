package com.example.backend.game.ai;

import com.example.backend.game.Move;

import java.util.Objects;

/**
 * Транспозиционная таблица на Zobrist-хэшах (ТЗ 6): кэш оценки и лучшего
 * хода с границами (EXACT/LOWER/UPPER для корректности альфа-беты).
 * Развёрнутый хэш-массив фиксированной (округлённой до степени двойки)
 * вместимости: переполнение перезаписывает слот более глубокой записью —
 * размер ограничен настраиваемой {@code ttCapacity}.
 */
public final class TranspositionTable {

    /** Граница оценки: EXACT — точное, LOWER/UPPER — нижняя/верхняя. */
    public enum Flag {
        EXACT, LOWER, UPPER
    }

    /** Запись: позиция (по хэшу), глубина, оценка (с поправкой на ply), граница, лучший ход. */
    public record Entry(long hash, int depth, int score, Flag flag, Move move) {

        public Entry {
            Objects.requireNonNull(flag, "flag");
        }
    }

    private final Entry[] table;
    private final int mask;

    /** @param capacity заявленная вместимость; округляется вверх до степени двойки, минимум 64 */
    public TranspositionTable(int capacity) {
        if (capacity < 64) {
            throw new IllegalArgumentException("вместимость TT не меньше 64: " + capacity);
        }
        int size = 64;
        while (size < capacity) {
            size <<= 1;
        }
        this.table = new Entry[size];
        this.mask = size - 1;
    }

    /** Фактическая вместимость (степень двойки). */
    public int capacity() {
        return table.length;
    }

    /** Запись для позиции или {@code null} (промах/чужой хэш в слоте). */
    public Entry probe(long hash) {
        Entry entry = table[(int) (hash & mask)];
        return entry != null && entry.hash() == hash ? entry : null;
    }

    /** Сохранение; более глубокая запись той же позиции не затирается более слабой. */
    public void store(long hash, int depth, int score, Flag flag, Move move) {
        int index = (int) (hash & mask);
        Entry existing = table[index];
        if (existing == null || existing.hash() != hash || existing.depth() <= depth) {
            table[index] = new Entry(hash, depth, score, flag, move);
        }
    }
}
