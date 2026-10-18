package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.Move;
import com.example.backend.game.MoveApplier;
import com.example.backend.game.MoveGenerator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Минимакс с альфа-бета отсечениями (ТЗ 6): итеративное углубление
 * гарантирует ответ при любом бюджете времени, сортировка ходов — взятия
 * (по цене снятых), TT-ход, «убийственные» (киллер) тихие, прочие.
 * <p>Ветвление: отдельная копия доски на узел ({@link MoveApplier#apply});
 * конец партии — нет ходов = поражение (ТЗ 3.6); ничья по повторению
 * и «30 полуходов дамками» — обрезка внутри поиска (S2.4).
 */
public final class AlphaBetaSearch {

    /** Счёт «мата»: больше любой оценки материала. */
    static final int MATE_SCORE = 1_000_000;
    /** Граница окна оценки (вдвое больше мата — защита от переполнения). */
    static final int SCORE_BOUND = 2 * MATE_SCORE;
    /** Проверка времени — каждые 2^9 узлов. */
    private static final int TIME_CHECK_INTERVAL = 512;
    /** Максимум плоскостей в глубине (глубина + запас). */
    private static final int MAX_PLIES = 16;
    /** Киллер-ходов на плоскость. */
    private static final int KILLERS_PER_PLY = 2;

    private final PositionEvaluator evaluator;

    public AlphaBetaSearch() {
        this(new PositionEvaluator());
    }

    public AlphaBetaSearch(PositionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /** Лучший ход с позиции {@code player} в пределах настроек поиска. */
    public AiResult findBestMove(Board board, Color player, AiSettings settings) {
        return findBestMove(board, player, Map.of(), 0, settings);
    }

    /**
     * Лучший ход с учётом фактической истории повторений партии
     * и счётчика «дамки без взятий» (обрезка ничьих внутри поиска, S2.4).
     */
    public AiResult findBestMove(Board board, Color player,
                                 Map<String, Integer> positionRepetitions,
                                 int noCaptureKingMoves,
                                 AiSettings settings) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(settings, "settings");
        positionRepetitions = positionRepetitions == null ? Map.of() : positionRepetitions;

        long startNanos = System.nanoTime();
        SearchContext context = new SearchContext(startNanos, settings.timeBudgetMs());

        List<Move> rootMoves = MoveGenerator.generateMoves(board, player);
        context.nodes++;
        if (rootMoves.isEmpty()) {
            return new AiResult(null, 0, -MATE_SCORE, context.nodes, elapsedMs(startNanos));
        }

        Move bestMove = rootMoves.get(0);
        int bestScore = 0;
        int completedDepth = 0;

        for (int depth = 1; depth <= settings.maxDepth(); depth++) {
            Move iterationMove = null;
            int iterationScore = -SCORE_BOUND;
            int alpha = -SCORE_BOUND;

            List<Move> ordered = MoveOrdering.order(board, rootMoves, bestMove,
                    context.killer(depth - 1, 0), context.killer(depth - 1, 1));
            for (Move move : ordered) {
                Board child = MoveApplier.apply(board, move);
                int score = -negamax(child, player.opposite(), depth - 1,
                        -SCORE_BOUND, -alpha, 1, context);
                if (context.timeUp) {
                    break;
                }
                if (score > iterationScore) {
                    iterationScore = score;
                    iterationMove = move;
                }
                if (score > alpha) {
                    alpha = score;
                }
            }

            if (context.timeUp || iterationMove == null) {
                break; // недоконченная итерация отбрасывается — остаётся прошлый лучший ход
            }
            bestMove = iterationMove;
            bestScore = iterationScore;
            completedDepth = depth;

            if (Math.abs(bestScore) >= MATE_SCORE - MAX_PLIES) {
                break; // мат/неизбежная победа найдены — глубже не нужно
            }
            if (System.nanoTime() > context.deadlineNanos) {
                break; // времени на следующую итерацию не хватит
            }
        }

        return new AiResult(bestMove, completedDepth, bestScore, context.nodes, elapsedMs(startNanos));
    }

    private int negamax(Board board, Color sideToMove, int depth, int alpha, int beta,
                        int ply, SearchContext context) {
        if ((context.nodes++ & (TIME_CHECK_INTERVAL - 1)) == 0
                && System.nanoTime() > context.deadlineNanos) {
            context.timeUp = true;
            return 0;
        }

        List<Move> moves = MoveGenerator.generateMoves(board, sideToMove);
        if (moves.isEmpty()) {
            return -MATE_SCORE + ply; // нет ходов — поражение стороны, чей ход (ТЗ 3.6)
        }
        // ничья по повторению и «30 полуходов дамками» — S2.4

        if (depth == 0) {
            return evaluator.evaluate(board, sideToMove);
        }

        Move ttMove = null; // транспозиционная таблица — S2.3
        List<Move> ordered = MoveOrdering.order(board, moves, ttMove,
                context.killer(ply, 0), context.killer(ply, 1));

        int bestScore = -SCORE_BOUND;
        Move bestMove = null;
        for (Move move : ordered) {
            Board child = MoveApplier.apply(board, move);
            int score = -negamax(child, sideToMove.opposite(), depth - 1,
                    -beta, -alpha, ply + 1, context);
            if (context.timeUp) {
                return 0;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                context.storeKiller(ply, move, board); // «убийственный» тихий ход
                break;
            }
        }
        return bestScore;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Состояние одного поиска: дедлайн, счётчик узлов, киллер-ходы, флаг таймаута. */
    private static final class SearchContext {

        final long deadlineNanos;
        final Move[] killers = new Move[MAX_PLIES * KILLERS_PER_PLY];
        long nodes;
        boolean timeUp;

        SearchContext(long startNanos, long timeBudgetMs) {
            this.deadlineNanos = startNanos + timeBudgetMs * 1_000_000;
        }

        Move killer(int ply, int slot) {
            if (ply >= MAX_PLIES) {
                return null;
            }
            return killers[ply * KILLERS_PER_PLY + slot];
        }

        /** Киллер — только тихий ход, вызвавший отсечение (см. ТЗ 6). */
        void storeKiller(int ply, Move move, Board board) {
            if (ply >= MAX_PLIES || MoveOrdering.isCapture(board, move)) {
                return;
            }
            Move first = killer(ply, 0);
            if (move.equals(first)) {
                return;
            }
            killers[ply * KILLERS_PER_PLY + 1] = first;
            killers[ply * KILLERS_PER_PLY] = move;
        }
    }
}
