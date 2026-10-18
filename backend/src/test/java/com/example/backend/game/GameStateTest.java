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
}
