package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Emotion;
import com.example.backend.game.ai.AiSettings;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import com.example.backend.session.GamePreferences;
import com.example.backend.session.GameSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LlmCommentRecorderTest {

    private static GameSession session(String llmModel, CommentaryStyle style) {
        GameState state = GameState.newGame(Color.BLACK);
        state.applyMove(List.of(new Cell(5, 0), new Cell(4, 1))); // ход белой шашки, moveNumber = 1
        GamePreferences prefs = new GamePreferences(
                com.example.backend.dto.game.PlayerColor.WHITE,
                com.example.backend.dto.game.Difficulty.EASY, null, null, style, llmModel);
        AiSettings aiSettings = new AiSettings(4, 1500, 65536);
        return new GameSession(state, prefs, aiSettings, Instant.now());
    }

    @Test
    void recordSavesCommentEntity() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentRecorder recorder = new LlmCommentRecorder(repository);

        GameSession session = session("qwen2.5:7b-instruct", CommentaryStyle.COMPETITIVE);
        LlmComment comment = new LlmComment(null, 1, "Отличный ход!", "qwen2.5:7b-instruct",
                CommentaryStyle.COMPETITIVE, Emotion.CONFIDENT, false, true);

        recorder.record(session, comment);

        verify(repository, times(1)).save(any(LlmCommentEntity.class));
    }

    @Test
    void recordMapsFieldsCorrectly() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentRecorder recorder = new LlmCommentRecorder(repository);

        UUID gameId = UUID.randomUUID();
        GameState state = GameState.newGame(Color.BLACK);
        state.applyMove(List.of(new Cell(5, 0), new Cell(4, 1)));
        GamePreferences prefs = new GamePreferences(
                com.example.backend.dto.game.PlayerColor.WHITE,
                com.example.backend.dto.game.Difficulty.EASY, null, null,
                CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct");
        AiSettings aiSettings = new AiSettings(4, 1500, 65536);
        GameSession session = new GameSession(state, prefs, aiSettings, Instant.now());

        LlmComment comment = new LlmComment(null, 1, "Тестовый комментарий", "qwen2.5:7b-instruct",
                CommentaryStyle.NEUTRAL, Emotion.NEUTRAL, false, true);

        recorder.record(session, comment);

        var captor = org.mockito.ArgumentCaptor.forClass(LlmCommentEntity.class);
        verify(repository, times(1)).save(captor.capture());
        LlmCommentEntity entity = captor.getValue();
        assertThat(entity.getGameId()).isEqualTo(session.id());
        assertThat(entity.getComment()).isEqualTo("Тестовый комментарий");
        assertThat(entity.getStyle()).isEqualTo("neutral");
        assertThat(entity.getEmotion()).isEqualTo("neutral");
        assertThat(entity.getLlmModel()).isEqualTo("qwen2.5:7b-instruct");
        assertThat(entity.getMoveNumber()).isEqualTo(1);
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void recordDoesNotThrowWhenRepositoryFails() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        org.mockito.Mockito.when(repository.save(any()))
                .thenThrow(new RuntimeException("DB down"));
        LlmCommentRecorder recorder = new LlmCommentRecorder(repository);

        GameSession session = session("qwen2.5:7b-instruct", CommentaryStyle.FRIENDLY_MASTER);
        LlmComment comment = new LlmComment(null, 1, "Ход сделан.", null,
                CommentaryStyle.FRIENDLY_MASTER, Emotion.NEUTRAL, true, true);

        // Не должно кидать исключение — best-effort
        recorder.record(session, comment);

        verify(repository, times(1)).save(any(LlmCommentEntity.class));
    }

    @Test
    void fallbackCommentIsSavedWithNullModel() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentRecorder recorder = new LlmCommentRecorder(repository);

        GameSession session = session(null, CommentaryStyle.NEUTRAL);
        LlmComment comment = new LlmComment(null, 1, "Ход сделан.", null,
                CommentaryStyle.NEUTRAL, Emotion.NEUTRAL, true, true);

        recorder.record(session, comment);

        var captor = org.mockito.ArgumentCaptor.forClass(LlmCommentEntity.class);
        verify(repository, times(1)).save(captor.capture());
        LlmCommentEntity entity = captor.getValue();
        assertThat(entity.getLlmModel()).isNull();
        assertThat(entity.getComment()).isEqualTo("Ход сделан.");
    }
}
