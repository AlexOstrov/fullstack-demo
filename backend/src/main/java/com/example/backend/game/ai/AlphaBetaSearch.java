package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.Move;
import com.example.backend.game.MoveApplier;
import com.example.backend.game.MoveGenerator;
import com.example.backend.game.Piece;

import java.util.HashMap;
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
    /** Порог «матового» счёта: ближе к мату, чем любая материальная оценка. */
    static final int MATE_THRESHOLD = MATE_SCORE - MAX_PLIES;

    private final PositionEvaluator evaluator;
    private final Zobrist zobrist = Zobrist.INSTANCE;

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
     *
     * @param positionRepetitions кратность позиций в партии <b>включая текущую</b>
     *                            (как {@code GameState.positionRepetitions()});
     *                            корень не дублируется в счётчике пути поиска
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
        SearchContext context = new SearchContext(startNanos, settings.timeBudgetMs(),
                new TranspositionTable(settings.ttCapacity()), positionRepetitions);

        List<Move> rootMoves = MoveGenerator.generateMoves(board, player);
        context.nodes++;
        if (rootMoves.isEmpty()) {
            return new AiResult(null, 0, -MATE_SCORE, context.nodes, elapsedMs(startNanos));
        }

        // Корневая позиция НЕ добавляется в pathPositions: её кратность уже учтена
        // в gameRepetitions (GameState.positionRepetitions включает текущую позицию).
        // Иначе корень считался бы дважды и линия с возвратом в него оценивалась бы
        // как ничья на одно повторение раньше реального (исправление ревью 4.1).

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
                        -SCORE_BOUND, -alpha, 1, nextKingMoveCounter(board, move, noCaptureKingMoves),
                        context);
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
                        int ply, int noCaptureKingMoves, SearchContext context) {
        if ((context.nodes++ & (TIME_CHECK_INTERVAL - 1)) == 0
                && System.nanoTime() > context.deadlineNanos) {
            context.timeUp = true;
            return 0;
        }

        List<Move> moves = MoveGenerator.generateMoves(board, sideToMove);
        if (moves.isEmpty()) {
            return -MATE_SCORE + ply; // нет ходов — поражение стороны, чей ход (ТЗ 3.6)
        }

        // обрезка ничьих внутри поиска (ТЗ 6): троекратное повторение позиции
        // и 30 полуходов только дамками без взятий (ТЗ 3.6) — ничья = 0
        String positionKey = board.positionKey(sideToMove);
        context.pathPositions.merge(positionKey, 1, Integer::sum);
        try {
            int occurrences = context.gameRepetitions.getOrDefault(positionKey, 0)
                    + context.pathPositions.get(positionKey);
            if (occurrences >= 3
                    || noCaptureKingMoves >= GameState.KING_ONLY_DRAW_HALF_MOVES) {
                return 0; // ничья сильнее любой статической оценки
            }

            if (depth == 0) {
                return evaluator.evaluate(board, sideToMove);
            }

            // транспозиционная таблица (ТЗ 6): хэш позиции, кэш оценки и лучшего хода
            long hash = zobrist.hash(board, sideToMove);
            TranspositionTable.Entry ttEntry = context.tt.probe(hash);
            Move ttMove = ttEntry != null ? ttEntry.move() : null;

            int alphaOrig = alpha;
            int betaOrig = beta;
            if (ttEntry != null && ttEntry.depth() >= depth) {
                int ttScore = fromTtScore(ttEntry.score(), ply);
                switch (ttEntry.flag()) {
                    case EXACT -> {
                        return ttScore;
                    }
                    case LOWER -> {
                        alpha = Math.max(alpha, ttScore);
                    }
                    case UPPER -> {
                        beta = Math.min(beta, ttScore);
                    }
                }
                if (alpha >= beta) {
                    return ttScore;
                }
            }

            List<Move> ordered = MoveOrdering.order(board, moves, ttMove,
                    context.killer(ply, 0), context.killer(ply, 1));

            int bestScore = -SCORE_BOUND;
            Move bestMove = null;
            for (Move move : ordered) {
                Board child = MoveApplier.apply(board, move);
                int score = -negamax(child, sideToMove.opposite(), depth - 1,
                        -beta, -alpha, ply + 1,
                        nextKingMoveCounter(board, move, noCaptureKingMoves), context);
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

            // сохранение: граница по результату ветвления (fail-low → UPPER, fail-high → LOWER)
            TranspositionTable.Flag flag = bestScore <= alphaOrig
                    ? TranspositionTable.Flag.UPPER
                    : bestScore >= betaOrig ? TranspositionTable.Flag.LOWER : TranspositionTable.Flag.EXACT;
            context.tt.store(hash, depth, toTtScore(bestScore, ply), flag, bestMove);
            return bestScore;
        } finally {
            context.pathPositions.merge(positionKey, -1, Integer::sum); // откат пути
        }
    }

    /** Счётчик «дамки без взятий» после хода: ход дамки без боя продолжает, иначе — сброс. */
    private static int nextKingMoveCounter(Board board, Move move, int current) {
        Piece moved = board.at(move.path().get(0).row(), move.path().get(0).col());
        boolean kingQuiet = moved.isKing() && !MoveOrdering.isCapture(board, move);
        return kingQuiet ? current + 1 : 0;
    }

    /**
     * Поправка «матовых» счётов при записи в TT: счёт зависит от плоскости
     * (быстрая победа лучше поздней) — приводится к глубине 0.
     */
    private static int toTtScore(int score, int ply) {
        if (score > MATE_THRESHOLD) {
            return score + ply;
        }
        if (score < -MATE_THRESHOLD) {
            return score - ply;
        }
        return score;
    }

    private static int fromTtScore(int score, int ply) {
        if (score > MATE_THRESHOLD) {
            return score - ply;
        }
        if (score < -MATE_THRESHOLD) {
            return score + ply;
        }
        return score;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Состояние одного поиска: дедлайн, счётчик узлов, TT, киллер-ходы, история повторений. */
    private static final class SearchContext {

        final long deadlineNanos;
        final TranspositionTable tt;
        /** Повторения из фактической истории партии (read-only), включая корень. */
        final Map<String, Integer> gameRepetitions;
        /** Повторения вдоль ветви поиска ниже корня (добавление/откат по возврату). */
        final Map<String, Integer> pathPositions = new HashMap<>();
        final Move[] killers = new Move[MAX_PLIES * KILLERS_PER_PLY];
        long nodes;
        boolean timeUp;

        SearchContext(long startNanos, long timeBudgetMs, TranspositionTable tt,
                      Map<String, Integer> gameRepetitions) {
            this.deadlineNanos = startNanos + timeBudgetMs * 1_000_000;
            this.tt = tt;
            this.gameRepetitions = gameRepetitions;
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
