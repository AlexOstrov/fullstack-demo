package com.example.backend.service;

import com.example.backend.dto.game.AiTurnDto;
import com.example.backend.dto.game.CommentDto;
import com.example.backend.dto.game.CommentSource;
import com.example.backend.dto.game.GameStateResponse;
import com.example.backend.dto.game.LegalMoveDto;
import com.example.backend.dto.game.MoveDto;
import com.example.backend.dto.game.MoveRecordDto;
import com.example.backend.dto.game.MovesResponse;
import com.example.backend.dto.game.PlayerColor;
import com.example.backend.game.Board;
import com.example.backend.game.GameState;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveValidator;
import com.example.backend.game.Piece;
import com.example.backend.session.GameSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Сборка ответов API из состояния партии (ТЗ 5.2). */
@Component
public class GameResponseMapper {

    /** Эмоция fallback-комментария (ТЗ 7.5): нейтральная по умолчанию. */
    private static final String FALLBACK_EMOTION = "neutral";

    /** Состояние партии без хода ИИ ({@code ai} = null). */
    public GameStateResponse toResponse(GameSession session) {
        return toResponse(session, null);
    }

    /** Состояние партии; {@code ai} заполняется только для ответа {@code ai-move}. */
    public GameStateResponse toResponse(GameSession session, AiTurnDto ai) {
        GameState state = session.state();
        boolean inProgress = !state.status().isTerminal();
        MoveValidator.Legal legal = inProgress
                ? MoveValidator.analyze(state.board(), state.currentPlayer())
                : new MoveValidator.Legal(List.of(), List.of());
        return new GameStateResponse(
                state.id(),
                PlayerColor.of(session.humanColor()),
                PlayerColor.of(session.aiColor()),
                state.status(),
                PlayerColor.of(state.currentPlayer()),
                state.moveNumber(),
                state.version(),
                boardTokens(state.board()),
                state.lastMove() == null ? null : MoveDto.from(state.lastMove()),
                legal.moves().stream().map(LegalMoveDto::from).toList(),
                legal.forcedCapture(),
                state.endReason(),
                ai);
    }

    /** Ход ИИ с комментарием fallback (ТЗ 5.2). */
    public AiTurnDto toAiTurn(Move move, LlmComment comment) {
        return new AiTurnDto(
                MoveDto.from(move),
                comment.text(),
                FALLBACK_EMOTION,
                comment.fallback() ? CommentSource.FALLBACK : CommentSource.LLM);
    }

    /** История ходов и комментариев (ТЗ 5, {@code GET /{id}/moves}). */
    public MovesResponse toMoves(GameSession session) {
        GameState state = session.state();
        List<Move> history = state.history();
        List<MoveRecordDto> moves = new ArrayList<>(history.size());
        for (int index = 0; index < history.size(); index++) {
            // Белые ходят первыми и ходы чередуются: нечётные номера — белые.
            PlayerColor player = index % 2 == 0 ? PlayerColor.WHITE : PlayerColor.BLACK;
            moves.add(MoveRecordDto.from(index + 1L, player, history.get(index)));
        }
        List<CommentDto> comments = state.comments().stream().map(CommentDto::from).toList();
        return new MovesResponse(moves, comments);
    }

    /** Доска компактными токенами: {@code ""}, {@code "w"}, {@code "W"}, {@code "b"}, {@code "B"} (ТЗ 5.2). */
    public List<List<String>> boardTokens(Board board) {
        List<List<String>> rows = new ArrayList<>(Board.SIZE);
        for (int row = 0; row < Board.SIZE; row++) {
            List<String> cells = new ArrayList<>(Board.SIZE);
            for (int col = 0; col < Board.SIZE; col++) {
                cells.add(token(board.at(row, col)));
            }
            rows.add(List.copyOf(cells));
        }
        return List.copyOf(rows);
    }

    private String token(Piece piece) {
        return switch (piece) {
            case EMPTY -> "";
            case WHITE_MAN -> "w";
            case WHITE_KING -> "W";
            case BLACK_MAN -> "b";
            case BLACK_KING -> "B";
        };
    }
}
