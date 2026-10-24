package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.Move;
import com.example.backend.game.MoveApplier;
import com.example.backend.game.MoveGenerator;
import com.example.backend.game.MoveType;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaSearchTest {

    private final AlphaBetaSearch search = new AlphaBetaSearch();

    @Test
    void returnsLegalMoveFromInitialPosition() {
        Board board = Board.initialPosition();

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.MEDIUM));

        assertNotNull(result.move());
        assertTrue(MoveGenerator.generateMoves(board, Color.WHITE).contains(result.move()),
                "ход ИИ обязан быть из списка допустимых ходов");
        assertTrue(result.depth() >= 1, "итеративное углубление завершает хотя бы первую итерацию");
        assertTrue(result.nodes() > 0);
    }

    @Test
    void capturesAreForcedSoQuietMovesAreIgnored() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(6, 5, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(2, 1), new Cell(0, 3)), result.move().path(),
                "при наличии боя генератор (и ИИ) предлагает только взятия (ТЗ 3.4)");
    }

    @Test
    void findsWinBySeriesCapturingAllOpponentPieces() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(6, 5, Piece.WHITE_MAN);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(4, 1), new Cell(2, 3), new Cell(0, 5)), result.move().path(),
                "серия снимает обе чёрные; чёрные без ходов — конец партии (ТЗ 3.6)");
        assertTrue(result.score() >= AlphaBetaSearch.MATE_SCORE - 16,
                "выигранная партия должна оцениваться как «мат»: " + result.score());
    }

    @Test
    void prefersCapturingKingOverMan() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);
        board.place(2, 3, Piece.BLACK_MAN);
        board.place(4, 3, Piece.BLACK_KING);

        AiResult result = search.findBestMove(board, Color.WHITE, AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(3, 2), new Cell(5, 4)), result.move().path(),
                "бой дамки (+300) материальнее боя шашки (+100)");
    }

    @Test
    void noLegalMoveReturnsNullWithLossScore() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);

        AiResult result = search.findBestMove(board, Color.BLACK, AiSettings.of(AiPreset.EASY));

        assertNull(result.move(), "у чёрных нет ни фигур, ни ходов");
        assertEquals(-AlphaBetaSearch.MATE_SCORE, result.score(),
                "нет ходов — поражение стороны, чей ход (ТЗ 3.6)");
    }

    @Test
    void smallTranspositionTableDoesNotBreakChoice() {
        Board board = Board.empty();
        board.place(3, 2, Piece.WHITE_MAN);
        board.place(2, 3, Piece.BLACK_MAN);
        board.place(4, 3, Piece.BLACK_KING);
        AiSettings settings = new AiSettings(2, AiSettings.MIN_TIME_BUDGET_MS, 64);

        AiResult result = search.findBestMove(board, Color.WHITE, settings);

        assertEquals(List.of(new Cell(3, 2), new Cell(5, 4)), result.move().path(),
                "тактический выбор не зависит от размера TT");
    }

    @Test
    void choosesDrawByRepetitionOverLosingLine() {
        Board board = Board.empty();
        board.place(5, 0, Piece.WHITE_MAN);
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(0, 3, Piece.BLACK_KING);

        // позиция после хода (5,0)→(4,1) уже встречалась дважды:
        // ещё одно вхождение — троекратное повторение и ничья (ТЗ 3.6)
        Board repeated = MoveApplier.apply(board,
                new Move(List.of(new Cell(5, 0), new Cell(4, 1)), MoveType.NORMAL));
        Map<String, Integer> history = Map.of(repeated.positionKey(Color.BLACK), 2);

        AiResult result = search.findBestMove(board, Color.WHITE, history, 0,
                AiSettings.of(AiPreset.EASY));

        assertEquals(List.of(new Cell(5, 0), new Cell(4, 1)), result.move().path(),
                "возврат в повторяющуюся позицию (ничья 0) лучше проигрышных линий (материал −200)");
        assertEquals(0, result.score(), "ничья внутри поиска оценивается в 0");
    }

    /**
     * Регрессия ревью (п. 4.1): корневая позиция не должна учитываться дважды.
     * Её кратность уже содержится в истории партии
     * ({@code GameState.positionRepetitions} включает текущую позицию), поэтому
     * указание корня в переданной истории со счётчиком 1 не должно менять
     * результат поиска: возврат в корень один раз — это ещё не троекратное
     * повторение (ТЗ 3.6). До исправления корень добавлялся и в счётчик пути
     * поиска, из-за чего такие линии ошибочно оценивались как ничья (0).
     */
    @Test
    void rootPositionIsNotCountedTwiceForRepetitionPruning() {
        Board board = Board.empty();
        board.place(0, 1, Piece.WHITE_KING);
        board.place(4, 3, Piece.BLACK_KING);
        board.place(5, 0, Piece.BLACK_MAN);
        String rootKey = board.positionKey(Color.WHITE);
        // глубина 4: возврат в корень возможен только один раз (иначе трёхкратность
        // недостижима в пределах горизонта), поэтому обе истории эквивалентны
        AiSettings settings = new AiSettings(4, AiSettings.MIN_TIME_BUDGET_MS, 64);

        AiResult withRoot = search.findBestMove(board, Color.WHITE, Map.of(rootKey, 1), 0, settings);
        AiResult withoutRoot = search.findBestMove(board, Color.WHITE, Map.of(), 0, settings);

        assertEquals(withoutRoot.score(), withRoot.score(),
                "корень, указанный в истории один раз, не должен трактоваться как ничья");
        assertEquals(withoutRoot.move(), withRoot.move());
    }

    @Test
    void kingOnlyCounterForcesDrawInsideSearch() {
        Board board = Board.empty();
        board.place(5, 0, Piece.WHITE_KING);
        board.place(0, 3, Piece.BLACK_KING);

        AiResult result = search.findBestMove(board, Color.WHITE, Map.of(),
                GameState.KING_ONLY_DRAW_HALF_MOVES - 1, AiSettings.of(AiPreset.EASY));

        assertNotNull(result.move());
        assertEquals(0, result.score(),
                "любой ход дамки без боя доводит счётчик до 30 — ничья (ТЗ 3.6)");
    }

    @Test
    void staysWithinTimeBudget() {
        Board board = Board.initialPosition();
        AiSettings settings = new AiSettings(AiSettings.MAX_DEPTH, 500,
                AiSettings.DEFAULT_TT_CAPACITY);

        AiResult result = search.findBestMove(board, Color.WHITE, settings);

        assertTrue(result.elapsedMs() <= 900,
                "бюджет 500 мс + запас на шум ОС: " + result.elapsedMs() + " мс");
        assertNotNull(result.move());
        assertTrue(result.depth() >= 1);
    }

    @Test
    void minimalBudgetStillAnswers() {
        Board board = Board.initialPosition();
        AiSettings settings = new AiSettings(AiSettings.MAX_DEPTH, AiSettings.MIN_TIME_BUDGET_MS,
                AiSettings.DEFAULT_TT_CAPACITY);

        AiResult result = search.findBestMove(board, Color.BLACK, settings);

        assertNotNull(result.move(), "итеративное углубление гарантирует ответ при любом бюджете");
        assertTrue(result.elapsedMs() <= 600,
                "минимальный бюджет 200 мс + запас: " + result.elapsedMs() + " мс");
    }

    /**
     * R-2 из ревью Sprint 0–2: если время вышло до завершения первой итерации,
     * ход всё равно валиден, но {@code depth = 0}, а {@code score} — статическая
     * оценка выбранного хода, а не фиктивный ноль.
     */
    @Test
    void abortedFirstIterationReturnsStaticScoreWithDepthZero() {
        long[] calls = {0};
        AlphaBetaSearch timed = new AlphaBetaSearch(new PositionEvaluator(), () -> {
            long value = calls[0];
            calls[0] = 1_000_000_000L; // все последующие обращения — «время вышло»
            return value;
        });
        Board board = Board.initialPosition();
        AiSettings settings = new AiSettings(4, AiSettings.MIN_TIME_BUDGET_MS, 64);

        AiResult result = timed.findBestMove(board, Color.WHITE, settings);

        assertNotNull(result.move());
        assertEquals(0, result.depth(), "первая итерация прервана — просмотренной глубины нет");
        int expected = -new PositionEvaluator().evaluate(
                MoveApplier.apply(board, result.move()), Color.BLACK);
        assertEquals(expected, result.score(),
                "score при depth = 0 — статическая оценка выбранного хода");
    }
}
