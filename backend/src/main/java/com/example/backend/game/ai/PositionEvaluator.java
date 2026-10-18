package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.MoveGenerator;
import com.example.backend.game.Piece;

/**
 * Эвристика оценки позиции (ТЗ 6).
 * <p>Компоненты: материал (шашка 100 / дамка 300 — фиксировано ТЗ),
 * продвижение простых к превращению, безопасность (бонус бортов и тыла,
 * штраф за шашки под боем), мобильность (разница числа доступных ходов
 * с учётом обязательного взятия), небольшой центральный бонус — только
 * для дамок, темп (очередь хода).
 * <p>Результат — с позиции стороны, чей ход: положительное значение
 * означает преимущество этой стороны.
 */
public final class PositionEvaluator {

    /** Материал простой шашки (ТЗ 6). */
    public static final int MAN_VALUE = 100;
    /** Материал дамки (ТЗ 6). */
    public static final int KING_VALUE = 300;
    /** Темп: небольшой постоянный бонус стороны, чей ход. */
    static final int TEMPO = 5;

    /** Бонус продвижения простой за каждую клетку к последней горизонтали. */
    static final int PROGRESS_STEP = 4;
    /** Бонус «борта» (col 0 и 7): меньше углов атаки. */
    static final int EDGE_BONUS = 8;
    /** Бонус «тыла»: собственные задние ряды. */
    static final int BACK_BONUS = 5;
    /** Штраф за шашку под боем. */
    static final int UNDER_FIRE_PENALTY = -20;
    /** Вес одного доступного хода в мобильности. */
    static final int MOBILITY_WEIGHT = 2;
    /** Шаг центрального бонуса дамки. */
    static final int KING_CENTER_STEP = 2;

    private static final int[][] DIAGONALS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    /** Оценка позиции с точки зрения стороны, чей ход. */
    public int evaluate(Board board, Color sideToMove) {
        int whiteScore = materialAndProgress(board)
                + safety(board)
                + kingCenter(board)
                + mobility(board);
        int sign = sideToMove == Color.WHITE ? 1 : -1;
        return sign * whiteScore + TEMPO;
    }

    /** Материал и продвижение простых к превращению (сумма от белых). */
    static int materialAndProgress(Board board) {
        int score = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (piece.isEmpty()) {
                    continue;
                }
                int value = piece.isKing()
                        ? KING_VALUE
                        : MAN_VALUE + progressBonus(piece.color(), row);
                score += piece.color() == Color.WHITE ? value : -value;
            }
        }
        return score;
    }

    private static int progressBonus(Color color, int row) {
        int progress = color == Color.WHITE ? Board.SIZE - 1 - row : row;
        return progress * PROGRESS_STEP;
    }

    /** Безопасность: борта и тыл — бонус, шашки под боем — штраф (сумма от белых). */
    static int safety(Board board) {
        int score = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (piece.isEmpty()) {
                    continue;
                }
                int value = 0;
                if (col == 0 || col == Board.SIZE - 1) {
                    value += EDGE_BONUS;
                }
                boolean ownBack = piece.color() == Color.WHITE
                        ? row >= Board.SIZE - 2 : row <= 1;
                if (ownBack) {
                    value += BACK_BONUS;
                }
                if (isUnderFire(board, row, col)) {
                    value += UNDER_FIRE_PENALTY;
                }
                score += piece.color() == Color.WHITE ? value : -value;
            }
        }
        return score;
    }

    /** Центральный бонус — только для дамок (сумма от белых). */
    static int kingCenter(Board board) {
        int score = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (!piece.isKing()) {
                    continue;
                }
                int bonus = KING_CENTER_STEP * (Board.SIZE - chebyshevToCenter(row, col));
                score += piece.color() == Color.WHITE ? bonus : -bonus;
            }
        }
        return score;
    }

    /** Центр — «расстояние» по Чебышёву в клетках×2: центральные клетки — 1, углы — 7. */
    private static int chebyshevToCenter(int row, int col) {
        int deltaRow = Math.abs(2 * row - (Board.SIZE - 1));
        int deltaCol = Math.abs(2 * col - (Board.SIZE - 1));
        return Math.max(deltaRow, deltaCol);
    }

    /** Мобильность: разница числа доступных ходов, обязательное взятие учтено генератором. */
    static int mobility(Board board) {
        int white = MoveGenerator.generateMoves(board, Color.WHITE).size();
        int black = MoveGenerator.generateMoves(board, Color.BLACK).size();
        return (white - black) * MOBILITY_WEIGHT;
    }

    /**
     * Шашка/дамка под боем: первый непустой по диагонали — соперник
     * (шашка-нападающий — только через соседнюю жертву, дамка — с любой
     * дистанции), и за жертвой есть свободная клетка приземления.
     */
    static boolean isUnderFire(Board board, int row, int col) {
        Piece victim = board.at(row, col);
        if (victim.isEmpty()) {
            return false;
        }
        Color attackerColor = victim.color().opposite();
        for (int[] diagonal : DIAGONALS) {
            int stepRow = diagonal[0];
            int stepCol = diagonal[1];

            int atkRow = row + stepRow;
            int atkCol = col + stepCol;
            while (Board.inBounds(atkRow, atkCol) && board.at(atkRow, atkCol).isEmpty()) {
                atkRow += stepRow;
                atkCol += stepCol;
            }
            if (!Board.inBounds(atkRow, atkCol)) {
                continue;
            }
            Piece first = board.at(atkRow, atkCol);
            if (!first.is(attackerColor)) {
                continue; // своя первая по диагонали — боя нет
            }
            if (first.isMan() && Math.abs(atkRow - row) != 1) {
                continue; // шашка бьёт только через соседнюю жертву
            }
            int landRow = row - stepRow;
            int landCol = col - stepCol;
            while (Board.inBounds(landRow, landCol) && board.at(landRow, landCol).isEmpty()) {
                return true; // есть приземление за жертвой — взятие возможно
            }
        }
        return false;
    }
}
