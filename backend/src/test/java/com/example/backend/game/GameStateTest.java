package com.example.backend.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    @Test
    void newGameInitializesStateSpecFields() {
        GameState game = GameState.newGame(Color.WHITE);

        assertTrue(game.id() != null);
        assertEquals(Color.WHITE, game.humanColor());
        assertEquals(Color.BLACK, game.aiColor());
        assertEquals(Color.WHITE, game.currentPlayer(), "белые ходят первыми (ТЗ 3.1)");
        assertEquals(GameStatus.IN_PROGRESS, game.status());
        assertEquals(0, game.moveNumber());
        assertEquals(0, game.version());
        assertNull(game.lastMove());
        assertTrue(game.history().isEmpty());
        assertTrue(game.comments().isEmpty());
        assertEquals(0, game.noCaptureKingMoves());
    }

    @Test
    void newGameWithBlackHumanStillStartsWithWhiteTurn() {
        GameState game = GameState.newGame(Color.BLACK);

        assertEquals(Color.BLACK, game.humanColor());
        assertEquals(Color.WHITE, game.aiColor());
        assertEquals(Color.WHITE, game.currentPlayer());
    }

    @Test
    void newGameStartsWithInitialPositionAndSingleRepetition() {
        GameState game = GameState.newGame(Color.WHITE);

        assertEquals(Board.initialPosition(), game.board());
        assertEquals(12, game.board().count(Color.WHITE));
        assertEquals(12, game.board().count(Color.BLACK));

        assertEquals(1, game.positionRepetitions().size());
        assertEquals(1, game.positionRepetitions().get(game.positionKey()));
        assertEquals(game.board().positionKey() + ":WHITE", game.positionKey());
    }

    @Test
    void eachGameHasUniqueId() {
        assertNotEquals(GameState.newGame(Color.WHITE).id(), GameState.newGame(Color.WHITE).id());
    }

    @Test
    void historyAndCommentsAreUnmodifiableViews() {
        GameState game = GameState.newGame(Color.WHITE);

        assertThrows(UnsupportedOperationException.class, () -> game.history().add(null));
        assertThrows(UnsupportedOperationException.class, () -> game.comments().add(null));
        assertThrows(UnsupportedOperationException.class, () -> game.positionRepetitions().clear());
    }

    @Test
    void movePathIsImmutableCopy() {
        List<Cell> path = new ArrayList<>(List.of(new Cell(6, 1), new Cell(5, 0)));
        Move move = new Move(path, MoveType.NORMAL);

        path.add(new Cell(4, 3)); // изменение исходного списка не влияет на Move
        assertEquals(2, move.path().size());
        assertThrows(UnsupportedOperationException.class, () -> move.path().add(new Cell(3, 2)));
    }

    // --- Применение ходов (ТЗ 4.4–4.5) ---

    @Test
    void applySimpleMoveUpdatesBoardAndCounters() {
        GameState game = GameState.newGame(Color.WHITE);

        game.applyMove(List.of(new Cell(5, 0), new Cell(4, 1)));

        assertTrue(game.board().at(5, 0).isEmpty());
        assertEquals(Piece.WHITE_MAN, game.board().at(4, 1));
        assertEquals(Color.BLACK, game.currentPlayer(), "игрок сменился");
        assertEquals(1, game.moveNumber());
        assertEquals(1, game.version());
        assertEquals(MoveType.NORMAL, game.lastMove().type());
        assertEquals(List.of(new Cell(5, 0), new Cell(4, 1)), game.lastMove().path());
        assertEquals(1, game.history().size());
        assertEquals(2, game.positionRepetitions().size());
        assertEquals(1, game.positionRepetitions().get(game.positionKey()));
    }

    @Test
    void applyCaptureSeriesRemovesVictimsAlongTheWay() {
        Board board = Board.empty();
        board.place(6, 1, Piece.WHITE_MAN);
        board.place(5, 2, Piece.BLACK_MAN);
        board.place(3, 4, Piece.BLACK_MAN);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        game.applyMove(List.of(new Cell(6, 1), new Cell(4, 3), new Cell(2, 5)));

        assertTrue(game.board().at(6, 1).isEmpty());
        assertTrue(game.board().at(5, 2).isEmpty(), "первая жертва снята по мере прыжка");
        assertTrue(game.board().at(3, 4).isEmpty(), "вторая жертва снята по мере прыжка");
        assertEquals(Piece.WHITE_MAN, game.board().at(2, 5));
        assertEquals(MoveType.CAPTURE, game.lastMove().type());
    }

    @Test
    void applyMovePromotesManOnLastRow() {
        Board whiteBoard = Board.empty();
        whiteBoard.place(1, 2, Piece.WHITE_MAN);
        GameState white = GameState.ofPosition(whiteBoard, Color.WHITE, Color.WHITE);
        white.applyMove(List.of(new Cell(1, 2), new Cell(0, 3)));
        assertEquals(Piece.WHITE_KING, white.board().at(0, 3), "белая превращается на row 0 (ТЗ 3.5)");
        assertEquals(MoveType.PROMOTION, white.lastMove().type());

        Board blackBoard = Board.empty();
        blackBoard.place(6, 5, Piece.BLACK_MAN);
        GameState black = GameState.ofPosition(blackBoard, Color.BLACK, Color.BLACK);
        black.applyMove(List.of(new Cell(6, 5), new Cell(7, 4)));
        assertEquals(Piece.BLACK_KING, black.board().at(7, 4), "чёрная превращается на row 7 (ТЗ 3.5)");
        assertEquals(MoveType.PROMOTION, black.lastMove().type());
    }

    @Test
    void applyMoveContinuesBattleAsKingAfterPromotion() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        // после превращения на (0,3) бой продолжается дамкой: приземление (3,6) — «через клетку»
        game.applyMove(List.of(new Cell(2, 1), new Cell(0, 3), new Cell(3, 6)));

        assertTrue(game.board().at(1, 2).isEmpty());
        assertTrue(game.board().at(1, 4).isEmpty());
        assertEquals(Piece.WHITE_KING, game.board().at(3, 6), "бой продолжился дамкой (ТЗ 3.5)");
        assertEquals(MoveType.PROMOTION, game.lastMove().type());
    }

    @Test
    void applyMoveCountsOnlyKingQuietMoves() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(6, 5, Piece.BLACK_MAN);
        board.place(1, 6, Piece.BLACK_MAN);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        game.applyMove(List.of(new Cell(4, 1), new Cell(0, 5))); // тихий ход дамки
        assertEquals(1, game.noCaptureKingMoves());

        game.applyMove(List.of(new Cell(6, 5), new Cell(7, 4))); // ход простой шашкой (с превращением)
        assertEquals(0, game.noCaptureKingMoves(), "ход простой шашкой обнуляет счётчик (ТЗ 3.6)");

        game.applyMove(List.of(new Cell(0, 5), new Cell(2, 7))); // взятие дамкой
        assertEquals(0, game.noCaptureKingMoves(), "взятие обнуляет счётчик даже у дамки");
        assertEquals(1, game.board().count(Color.BLACK), "жертва снята");
    }

    @Test
    void applyMoveRejectsPathAddressedToOpponentPiece() {
        GameState game = GameState.newGame(Color.WHITE);

        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> game.applyMove(List.of(new Cell(2, 1), new Cell(3, 0))));

        assertTrue(error.getMessage().contains("чужой шашке"));
        assertEquals(0, game.moveNumber(), "состояние не изменилось");
        assertEquals(Color.WHITE, game.currentPlayer());
    }

    @Test
    void applyMoveCountsRepeatedPosition() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 6, Piece.BLACK_KING);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);
        String initialKey = game.positionKey();

        // туда-обратно обеими дамками — позиция повторилась
        game.applyMove(List.of(new Cell(4, 1), new Cell(5, 0)));
        game.applyMove(List.of(new Cell(3, 6), new Cell(2, 7)));
        game.applyMove(List.of(new Cell(5, 0), new Cell(4, 1)));
        game.applyMove(List.of(new Cell(2, 7), new Cell(3, 6)));

        assertEquals(2, game.positionRepetitions().get(initialKey),
                "троекратное повторение считается по таблице (ТЗ 3.6)");
    }

    // --- Конец партии (ТЗ 3.6) ---

    @Test
    void gameEndsWhenOpponentHasNoPieces() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        game.applyMove(List.of(new Cell(2, 1), new Cell(0, 3))); // взятие с превращением

        assertEquals(0, game.board().count(Color.BLACK));
        assertEquals(GameStatus.WHITE_WON, game.status(), "нет ходов у соперника — победа (ТЗ 3.6)");

        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> game.applyMove(List.of(new Cell(0, 3), new Cell(1, 4))));
        assertTrue(error.getMessage().contains("завершена"), "ход после конца партии запрещён");
    }

    @Test
    void gameEndsWhenSideIsCompletelyBlocked() {
        Board board = Board.empty();
        board.place(5, 0, Piece.WHITE_MAN);
        board.place(7, 2, Piece.BLACK_MAN); // на последней горизонтали: ходить некуда
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        game.applyMove(List.of(new Cell(5, 0), new Cell(4, 1)));

        assertEquals(GameStatus.WHITE_WON, game.status(),
                "полностью заблокированная сторона проигрывает (ТЗ 3.6)");
    }

    @Test
    void drawOnThirdRepetitionOfSamePosition() {
        Board board = Board.empty();
        board.place(4, 1, Piece.WHITE_KING);
        board.place(3, 6, Piece.BLACK_KING);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);
        String initialKey = game.positionKey();

        // «туда-обратно» обеими дамками: позиция повторяется каждые два хода
        for (int i = 0; i < 2; i++) {
            game.applyMove(List.of(new Cell(4, 1), new Cell(5, 0)));
            game.applyMove(List.of(new Cell(3, 6), new Cell(2, 7)));
            assertEquals(GameStatus.IN_PROGRESS, game.status(), "первое повторение ещё не ничья");
            game.applyMove(List.of(new Cell(5, 0), new Cell(4, 1)));
            game.applyMove(List.of(new Cell(2, 7), new Cell(3, 6)));
        }

        assertEquals(3, game.positionRepetitions().get(initialKey));
        assertEquals(GameStatus.DRAW, game.status(), "троекратное повторение — ничья (ТЗ 3.6)");
    }

    @Test
    void drawAfterThirtyKingOnlyHalfMoves() {
        Board board = Board.empty();
        board.place(6, 3, Piece.WHITE_KING);
        board.place(1, 2, Piece.BLACK_KING);
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        // Дамки ходят по истинным циклам на диагоналях, не имеющих общих клеток
        // и линий: взятий и троекратного повторения (до 30 полуходов) не происходит,
        // а период комбинированной позиции LCM(4,6)=12 ходов даёт третье повторение
        // только на 24-м ходе — позже порога ничьей.
        Cell[] whiteCycle = {new Cell(6, 3), new Cell(7, 4), new Cell(6, 5), new Cell(5, 4)};
        Cell[] blackCycle = {new Cell(1, 2), new Cell(2, 3), new Cell(3, 4),
                new Cell(2, 5), new Cell(1, 4), new Cell(0, 3)};

        for (int i = 0; i < 15; i++) {
            game.applyMove(List.of(whiteCycle[i % 4], whiteCycle[(i + 1) % 4]));
            game.applyMove(List.of(blackCycle[i % 6], blackCycle[(i + 1) % 6]));
            if (i < 14) {
                assertEquals(GameStatus.IN_PROGRESS, game.status(),
                        "ничьей раньше 30 полуходов только дамками быть не должно");
            }
        }

        assertEquals(30, game.noCaptureKingMoves());
        assertEquals(GameStatus.DRAW, game.status(), "30 полуходов подряд только дамками (ТЗ 3.6)");
    }

    // --- Сдача и ничья по согласию (ТЗ 3.6); учёт предложений — API-слой (Sprint 3) ---

    @Test
    void resignFixesDefeatOfResigningPlayer() {
        GameState game = GameState.newGame(Color.WHITE);
        long versionBefore = game.version();

        game.resign(Color.WHITE);

        assertEquals(GameStatus.BLACK_WON, game.status(), "сдавшийся проигрывает (ТЗ 3.6)");
        assertTrue(game.version() > versionBefore, "смена статуса увеличивает версию состояния");

        InvalidMoveException error = assertThrows(InvalidMoveException.class,
                () -> game.applyMove(List.of(new Cell(5, 0), new Cell(4, 1))));
        assertTrue(error.getMessage().contains("завершена"), "ход после сдачи запрещён");
    }

    @Test
    void drawByAgreementFixesDraw() {
        GameState game = GameState.newGame(Color.BLACK);

        game.drawByAgreement();

        assertEquals(GameStatus.DRAW, game.status(), "ничья по согласию (ТЗ 3.6)");
        assertThrows(InvalidMoveException.class, () -> game.resign(Color.WHITE),
                "после завершения действия невозможны");
    }

    // --- «Турецкий удар» (ТЗ 12) ---

    @Test
    void applyMoveTurkishStrikeSeriesReturnsToStartCell() {
        Board board = Board.empty();
        board.place(2, 1, Piece.WHITE_MAN);
        board.place(1, 2, Piece.BLACK_MAN);
        board.place(1, 4, Piece.BLACK_MAN);
        board.place(3, 4, Piece.BLACK_MAN);
        board.place(3, 2, Piece.BLACK_MAN);
        board.place(3, 0, Piece.WHITE_MAN);
        board.place(3, 6, Piece.WHITE_MAN);
        board.place(7, 4, Piece.BLACK_KING); // выжившая дамка: партия продолжается
        GameState game = GameState.ofPosition(board, Color.WHITE, Color.WHITE);

        // серия возвращается на исходную клетку (2,1) — «турецкий удар» (ТЗ 3.4)
        game.applyMove(List.of(new Cell(2, 1), new Cell(0, 3), new Cell(2, 5),
                new Cell(4, 3), new Cell(2, 1)));

        assertEquals(1, game.board().count(Color.BLACK), "все четыре шашки взяты по мере прыжка, дамка уцелела");
        assertTrue(game.board().at(1, 2).isEmpty());
        assertTrue(game.board().at(1, 4).isEmpty());
        assertTrue(game.board().at(3, 4).isEmpty());
        assertTrue(game.board().at(3, 2).isEmpty());
        assertEquals(Piece.WHITE_KING, game.board().at(2, 1),
                "шашка вернулась на посещённую клетку и превратилась в дамку (ТЗ 3.5)");
        assertEquals(MoveType.PROMOTION, game.lastMove().type());
        assertEquals(Color.BLACK, game.currentPlayer());
        assertEquals(GameStatus.IN_PROGRESS, game.status(), "у чёрных остаются ходы дамками");
    }
}
