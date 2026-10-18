package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Color;
import com.example.backend.game.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionEvaluatorTest {

    private final PositionEvaluator evaluator = new PositionEvaluator();

    @Test
    void emptyBoardScoresOnlyTempo() {
        assertEquals(PositionEvaluator.TEMPO, evaluator.evaluate(Board.empty(), Color.WHITE));
        assertEquals(PositionEvaluator.TEMPO, evaluator.evaluate(Board.empty(), Color.BLACK),
                "темп — постоянный бонус стороны, чей ход");
    }

    @Test
    void symmetricPositionIsEqualForBothSides() {
        Board board = Board.empty();
        board.place(7, 0, Piece.WHITE_MAN);
        board.place(0, 7, Piece.BLACK_MAN);

        assertEquals(PositionEvaluator.TEMPO, evaluator.evaluate(board, Color.WHITE),
                "зеркальная позиция: все компоненты взаимно гасятся, остаётся только темп");
        assertEquals(PositionEvaluator.TEMPO, evaluator.evaluate(board, Color.BLACK));
    }

    @Test
    void evaluationIsAntiSymmetricWithoutTempo() {
        Board board = Board.empty();
        board.place(5, 0, Piece.WHITE_MAN);
        board.place(4, 1, Piece.WHITE_MAN);
        board.place(2, 5, Piece.BLACK_KING);

        int forWhite = evaluator.evaluate(board, Color.WHITE) - PositionEvaluator.TEMPO;
        int forBlack = evaluator.evaluate(board, Color.BLACK) - PositionEvaluator.TEMPO;

        assertEquals(-forWhite, forBlack,
                "оценка из позиции соперника — та же, с точностью до знака");
    }

    @Test
    void extraManWorthHundredAndChange() {
        Board board = Board.empty();
        board.place(7, 0, Piece.WHITE_MAN);
        board.place(7, 2, Piece.WHITE_MAN);
        board.place(5, 2, Piece.WHITE_MAN); // «лишняя» шашка
        board.place(0, 7, Piece.BLACK_MAN);
        board.place(0, 5, Piece.BLACK_MAN);

        assertEquals(112 + PositionEvaluator.TEMPO, evaluator.evaluate(board, Color.WHITE),
                "лишняя шашка: +100 материала, +8 продвижения (row 5), +4 мобильности (2 хода ×2)");
    }

    @Test
    void kingOutweighsManByMaterialCenterAndMobility() {
        Board withMan = Board.empty();
        withMan.place(7, 2, Piece.WHITE_MAN);
        withMan.place(0, 5, Piece.BLACK_MAN);
        Board withKing = Board.empty();
        withKing.place(7, 2, Piece.WHITE_KING);
        withKing.place(0, 5, Piece.BLACK_MAN);

        int delta = evaluator.evaluate(withKing, Color.WHITE)
                - evaluator.evaluate(withMan, Color.WHITE);
        assertEquals(212, delta,
                "дамка против шашки: +200 материала, +2 центра (2×(8−7)), +10 мобильности (5 ходов ×2)");
    }

    @Test
    void progressTowardPromotionAddsSixteen() {
        Board deep = Board.empty();
        deep.place(1, 2, Piece.WHITE_MAN);
        deep.place(0, 7, Piece.BLACK_MAN);
        Board shallow = Board.empty();
        shallow.place(5, 2, Piece.WHITE_MAN);
        shallow.place(0, 7, Piece.BLACK_MAN);

        int delta = evaluator.evaluate(deep, Color.WHITE)
                - evaluator.evaluate(shallow, Color.WHITE);
        assertEquals(16, delta,
                "шашка у превращения (row 1) ценится на 4×4 выше, чем в своём ряду (row 5)");
    }

    @Test
    void edgeBonusPartlyOffsetByMobility() {
        Board edge = Board.empty();
        edge.place(3, 0, Piece.WHITE_MAN);
        edge.place(0, 7, Piece.BLACK_MAN);
        Board plain = Board.empty();
        plain.place(3, 2, Piece.WHITE_MAN);
        plain.place(0, 7, Piece.BLACK_MAN);

        int delta = evaluator.evaluate(edge, Color.WHITE) - evaluator.evaluate(plain, Color.WHITE);
        assertEquals(6, delta, "борт: +8 за col 0, минус −2 мобильности (у борта одна диагональ)");
    }

    @Test
    void underFirePenalizesPiece() {
        Board attacked = Board.empty();
        attacked.place(3, 2, Piece.WHITE_MAN);
        attacked.place(5, 4, Piece.BLACK_KING);
        Board safe = Board.empty();
        safe.place(3, 2, Piece.WHITE_MAN);
        safe.place(5, 6, Piece.BLACK_KING);

        assertTrue(evaluator.evaluate(attacked, Color.WHITE) < evaluator.evaluate(safe, Color.WHITE),
                "шашка под боем дамки: штраф −20 перевешивает прочие различия позиций");
    }

    @Test
    void underFireDetection() {
        Board kingAttacker = Board.empty();
        kingAttacker.place(3, 2, Piece.WHITE_MAN);
        kingAttacker.place(6, 5, Piece.BLACK_KING);
        assertTrue(PositionEvaluator.isUnderFire(kingAttacker, 3, 2),
                "дамка бьёт через любую дистанцию");
        assertFalse(PositionEvaluator.isUnderFire(kingAttacker, 6, 5),
                "у белой шашки нет взятия через три — чёрная не под боем");

        Board manAttacker = Board.empty();
        manAttacker.place(3, 2, Piece.WHITE_MAN);
        manAttacker.place(4, 3, Piece.BLACK_MAN);
        assertTrue(PositionEvaluator.isUnderFire(manAttacker, 3, 2),
                "шашка бьёт через соседнюю жертву");

        Board manTooFar = Board.empty();
        manTooFar.place(3, 2, Piece.WHITE_MAN);
        manTooFar.place(6, 5, Piece.BLACK_MAN);
        assertFalse(PositionEvaluator.isUnderFire(manTooFar, 3, 2),
                "шашка не бьёт с дистанции 3");

        Board manAtDistanceTwo = Board.empty();
        manAtDistanceTwo.place(3, 2, Piece.WHITE_MAN);
        manAtDistanceTwo.place(5, 4, Piece.BLACK_MAN);
        assertFalse(PositionEvaluator.isUnderFire(manAtDistanceTwo, 3, 2),
                "шашка бьёт только через соседнюю — дистанция 2 слишком велика");

        Board landingBlocked = Board.empty();
        landingBlocked.place(3, 2, Piece.WHITE_MAN);
        landingBlocked.place(2, 1, Piece.WHITE_MAN); // приземление за жертвой закрыто
        landingBlocked.place(5, 4, Piece.BLACK_KING);
        assertFalse(PositionEvaluator.isUnderFire(landingBlocked, 3, 2),
                "без свободного приземления взятия нет");

        Board landingAtEdge = Board.empty();
        landingAtEdge.place(0, 1, Piece.WHITE_MAN);
        landingAtEdge.place(2, 3, Piece.BLACK_KING);
        assertFalse(PositionEvaluator.isUnderFire(landingAtEdge, 0, 1),
                "за жертвой край доски — приземления нет");

        Board ownBlocks = Board.empty();
        ownBlocks.place(3, 2, Piece.WHITE_MAN);
        ownBlocks.place(4, 3, Piece.WHITE_MAN); // своя первая по диагонали
        ownBlocks.place(5, 6, Piece.BLACK_KING);
        assertFalse(PositionEvaluator.isUnderFire(ownBlocks, 3, 2),
                "своя первая по диагонали — боя нет");
    }

    @Test
    void centerDoesNotHelpMen() {
        Board centerMan = Board.empty();
        centerMan.place(3, 4, Piece.WHITE_MAN);
        centerMan.place(0, 3, Piece.BLACK_KING);
        Board plainMan = Board.empty();
        plainMan.place(3, 2, Piece.WHITE_MAN);
        plainMan.place(0, 3, Piece.BLACK_KING);

        assertEquals(evaluator.evaluate(plainMan, Color.WHITE), evaluator.evaluate(centerMan, Color.WHITE),
                "для простых шашек центр не поощряется (ТЗ 6): одинаковый ряд, равная мобильность");
    }

    @Test
    void kingPrefersCenter() {
        Board center = Board.empty();
        center.place(3, 4, Piece.WHITE_KING);
        center.place(0, 3, Piece.BLACK_KING);
        Board plain = Board.empty();
        plain.place(5, 2, Piece.WHITE_KING);
        plain.place(0, 3, Piece.BLACK_KING);

        assertTrue(evaluator.evaluate(center, Color.WHITE) > evaluator.evaluate(plain, Color.WHITE),
                "дамке даётся небольшой центральный бонус (+4 центра и +2 мобильности)");
    }
}
