package com.example.backend.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Единый генератор ходов (ТЗ 4.6) — общий для валидатора и минимакса.
 *
 * Правила (ТЗ 3):
 * <ul>
 *   <li>простая шашка ходит по диагонали вперёд на одну клетку, бьёт вперёд и назад (3.2);</li>
 *   <li>дамка ходит на любое количество свободных клеток, бьёт через ровно одну
 *       соперника с приземлением на любую свободную клетку до следующей занятой (3.3);</li>
 *   <li>взятие обязательное: при наличии взятий простые ходы не генерируются (3.4);</li>
 *   <li>правило большинства: из всех взятий допустимы только те, что берут максимальное
 *       число шашек (3.4) — см. {@link #generateMaxCaptures};</li>
 *   <li>взятая шашка снимается с доски по мере прыжка — серия идёт до упора (3.4);</li>
 *   <li>превращение происходит немедленно и бой продолжается как дамкой (3.5).</li>
 * </ul>
 */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    /** Все допустимые ходы игрока: при наличии взятий — только взятия (с учётом правила большинства). */
    public static List<Move> generateMoves(Board board, Color player) {
        List<Move> captures = generateMaxCaptures(board, player);
        if (!captures.isEmpty()) {
            return captures;
        }
        return generateSimpleMoves(board, player);
    }

    public static boolean hasCaptures(Board board, Color player) {
        return !generateCaptures(board, player).isEmpty();
    }

    /** Взятия, отфильтрованные правилом большинства (ТЗ 3.4): остаются только серии с максимальным числом взятий. */
    public static List<Move> generateMaxCaptures(Board board, Color player) {
        List<Move> captures = generateCaptures(board, player);
        if (captures.isEmpty()) {
            return captures;
        }
        int maxLength = captures.stream()
                .mapToInt(move -> move.path().size())
                .max()
                .orElse(0);
        return captures.stream()
                .filter(move -> move.path().size() == maxLength)
                .toList();
    }

    /** Простые ходы (без взятий): путь из двух клеток. */
    public static List<Move> generateSimpleMoves(Board board, Color player) {
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (piece.isEmpty() || !piece.is(player)) {
                    continue;
                }
                Cell from = new Cell(row, col);
                if (piece.isKing()) {
                    addKingSimpleMoves(board, from, moves);
                } else {
                    addManSimpleMoves(board, player, from, moves);
                }
            }
        }
        return moves;
    }

    /** Все серии взятий (полные пути до упора). */
    public static List<Move> generateCaptures(Board board, Color player) {
        List<Move> results = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.at(row, col);
                if (piece.isEmpty() || !piece.is(player)) {
                    continue;
                }
                Cell from = new Cell(row, col);
                // Поиск мутирует доску и откатывает изменения в finally —
                // исходная доска вызывающего остаётся неизменной.
                searchCaptures(board, from, piece, player,
                        new ArrayList<>(List.of(from)), false, results);
            }
        }
        return results;
    }

    // --- Простые ходы ---

    private static void addManSimpleMoves(Board board, Color player, Cell from, List<Move> moves) {
        int forward = player == Color.WHITE ? -1 : 1; // белые «вверх» (ТЗ 3.1)
        for (int dc = -1; dc <= 1; dc += 2) {
            Cell to = new Cell(from.row() + forward, from.col() + dc);
            if (Board.inBounds(to.row(), to.col()) && board.at(to.row(), to.col()).isEmpty()) {
                MoveType type = player.isPromotionRow(to.row())
                        ? MoveType.PROMOTION // ход с превращением без боя (ТЗ 3.5)
                        : MoveType.NORMAL;
                moves.add(new Move(List.of(from, to), type));
            }
        }
    }

    private static void addKingSimpleMoves(Board board, Cell from, List<Move> moves) {
        for (int[] direction : DIRECTIONS) {
            int row = from.row() + direction[0];
            int col = from.col() + direction[1];
            while (Board.inBounds(row, col) && board.at(row, col).isEmpty()) {
                moves.add(new Move(List.of(from, new Cell(row, col)), MoveType.NORMAL));
                row += direction[0];
                col += direction[1];
            }
        }
    }

    // --- Взятия ---

    private static void searchCaptures(Board board, Cell from, Piece piece, Color player,
                                       List<Cell> path, boolean promoted, List<Move> results) {
        List<CaptureStep> steps = captureSteps(board, from, piece, player);

        if (steps.isEmpty()) {
            if (path.size() >= 2) {
                results.add(new Move(path, promoted ? MoveType.PROMOTION : MoveType.CAPTURE));
            }
            return;
        }

        for (CaptureStep step : steps) {
            Piece captured = board.at(step.captured().row(), step.captured().col());
            board.remove(from.row(), from.col());
            board.remove(step.captured().row(), step.captured().col());

            Piece moved = piece;
            boolean nowPromoted = promoted;
            if (piece.isMan() && piece.color().isPromotionRow(step.to().row())) {
                moved = Piece.king(piece.color()); // превращение немедленно, бой продолжается дамкой (ТЗ 3.5)
                nowPromoted = true;
            }
            board.place(step.to().row(), step.to().col(), moved);

            try {
                List<Cell> nextPath = new ArrayList<>(path);
                nextPath.add(step.to());
                searchCaptures(board, step.to(), moved, player, nextPath, nowPromoted, results);
            } finally {
                // откат: вернуть снятую шашку и исходную фигуру на место
                board.remove(step.to().row(), step.to().col());
                board.place(step.captured().row(), step.captured().col(), captured);
                board.place(from.row(), from.col(), piece);
            }
        }
    }

    private static List<CaptureStep> captureSteps(Board board, Cell from, Piece piece, Color player) {
        List<CaptureStep> steps = new ArrayList<>();
        if (piece.isKing()) {
            addKingCaptureSteps(board, from, player, steps);
        } else {
            addManCaptureSteps(board, from, player, steps);
        }
        return steps;
    }

    private static void addManCaptureSteps(Board board, Cell from, Color player, List<CaptureStep> steps) {
        for (int[] direction : DIRECTIONS) {
            int victimRow = from.row() + direction[0];
            int victimCol = from.col() + direction[1];
            int landRow = from.row() + 2 * direction[0];
            int landCol = from.col() + 2 * direction[1];

            if (!Board.inBounds(landRow, landCol)) {
                continue;
            }
            Piece victim = board.at(victimRow, victimCol);
            if (victim.isEmpty() || victim.is(player)) {
                continue;
            }
            if (!board.at(landRow, landCol).isEmpty()) {
                continue;
            }
            steps.add(new CaptureStep(new Cell(landRow, landCol), new Cell(victimRow, victimCol)));
        }
    }

    private static void addKingCaptureSteps(Board board, Cell from, Color player, List<CaptureStep> steps) {
        for (int[] direction : DIRECTIONS) {
            int row = from.row() + direction[0];
            int col = from.col() + direction[1];

            // первая непустая клетка по диагонали — потенциальная жертва
            while (Board.inBounds(row, col) && board.at(row, col).isEmpty()) {
                row += direction[0];
                col += direction[1];
            }
            if (!Board.inBounds(row, col)) {
                continue;
            }
            Piece victim = board.at(row, col);
            if (victim.is(player)) {
                continue;
            }

            // приземления: любая свободная клетка за жертвой до следующей занятой (ТЗ 3.3)
            int landRow = row + direction[0];
            int landCol = col + direction[1];
            while (Board.inBounds(landRow, landCol) && board.at(landRow, landCol).isEmpty()) {
                steps.add(new CaptureStep(new Cell(landRow, landCol), new Cell(row, col)));
                landRow += direction[0];
                landCol += direction[1];
            }
        }
    }

    // --- Вспомогательные ---

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private record CaptureStep(Cell to, Cell captured) {
    }
}
