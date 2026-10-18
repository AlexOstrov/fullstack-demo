package com.example.backend.game;

import java.util.List;

/**
 * Валидация ходов (ТЗ 4.4) поверх единого генератора (ТЗ 4.6).
 *
 * Путь считается допустимым, если он совпадает с одним из сгенерированных
 * ходов: это гарантирует, что сервер и ИИ применяют одни и те же правила
 * (обязательное взятие, правило большинства, корректность промежуточных точек).
 */
public final class MoveValidator {

    private MoveValidator() {
    }

    /**
     * Допустимые ходы позиции для игрока.
     *
     * @param moves   все допустимые ходы; при наличии взятий — только взятия,
     *                отфильтрованные правилом большинства
     * @param captures из них — ходы-взятия (пусто, если взятий нет)
     */
    public record Legal(List<Move> moves, List<Move> captures) {

        public Legal {
            moves = List.copyOf(moves);
            captures = List.copyOf(captures);
        }

        /** Есть ли в позиции обязательные взятия (ТЗ 3.4). */
        public boolean forcedCapture() {
            return !captures.isEmpty();
        }

        /** Допустимый ход с таким путём или null. */
        public Move find(List<Cell> path) {
            return moves.stream()
                    .filter(move -> move.path().equals(path))
                    .findFirst()
                    .orElse(null);
        }
    }

    /** Допустимые ходы позиции для игрока (правило большинства уже применено). */
    public static Legal analyze(Board board, Color player) {
        List<Move> captures = MoveGenerator.generateMaxCaptures(board, player);
        if (!captures.isEmpty()) {
            return new Legal(captures, captures);
        }
        return new Legal(MoveGenerator.generateSimpleMoves(board, player), List.of());
    }

    /**
     * Валидация пути как одного хода (ТЗ 4.3–4.4): форма пути, адресация своей
     * шашке, принадлежность списку допустимых ходов.
     *
     * @return найденный допустимый ход (его тип — NORMAL, CAPTURE или PROMOTION)
     * @throws InvalidMoveException с причиной отказа
     */
    public static Move validate(Board board, Color player, List<Cell> path) {
        checkPathShape(path);

        Piece start = board.at(path.get(0).row(), path.get(0).col());
        if (start.isEmpty()) {
            throw new InvalidMoveException("на начальной клетке пути нет шашки");
        }
        if (!start.is(player)) {
            throw new InvalidMoveException("ход адресован чужой шашке");
        }

        Move matched = analyze(board, player).find(path);
        if (matched == null) {
            throw new InvalidMoveException("ход не соответствует правилам: возможно, "
                    + "пропущено обязательное взятие или нарушено правило большинства");
        }
        return matched;
    }

    private static void checkPathShape(List<Cell> path) {
        if (path == null || path.size() < 2) {
            throw new InvalidMoveException("ход должен содержать хотя бы две клетки");
        }
        for (Cell cell : path) {
            if (cell == null) {
                throw new InvalidMoveException("путь содержит пустую клетку");
            }
            if (!Board.inBounds(cell.row(), cell.col())) {
                throw new InvalidMoveException(
                        "клетка пути вне доски: row=" + cell.row() + ", col=" + cell.col());
            }
        }
        for (int i = 1; i < path.size(); i++) {
            Cell from = path.get(i - 1);
            Cell to = path.get(i);
            int dr = Math.abs(to.row() - from.row());
            int dc = Math.abs(to.col() - from.col());
            if (dr != dc || dr == 0) {
                throw new InvalidMoveException("шаг пути " + from + " -> " + to + " не по диагонали");
            }
        }
    }
}
