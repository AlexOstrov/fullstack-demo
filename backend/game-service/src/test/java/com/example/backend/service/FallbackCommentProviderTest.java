package com.example.backend.service;

import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.Emotion;
import com.example.backend.dto.game.PlayerColor;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.GameState;
import com.example.backend.game.LlmComment;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import com.example.backend.game.ai.AiSettings;
import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import com.example.backend.session.GamePreferences;
import com.example.backend.session.GameSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** B3: случайный сохранённый комментарий как fallback при недоступности LLM. */
class FallbackCommentProviderTest {

    private static final Move NORMAL_MOVE =
            new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL);

    private static GameSession session(CommentaryStyle style) {
        GameState state = GameState.newGame(Color.BLACK);
        GamePreferences prefs = new GamePreferences(
                PlayerColor.WHITE, Difficulty.EASY, null, null, style, "qwen2.5:7b-instruct");
        return new GameSession(state, prefs, new AiSettings(4, 1500, 64), Instant.now());
    }

    private static LlmCommentEntity stored(String text, String style) {
        return new LlmCommentEntity(
                UUID.randomUUID(), text, style, "neutral",
                "qwen2.5:7b-instruct", 5, Instant.now());
    }

    @Test
    void usesStoredCommentOfMatchingStyle() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        when(repository.findByStyleAndLikedTrueAndLlmModelIsNotNull("competitive"))
                .thenReturn(List.of(stored("Сохранённая живая реплика.", "competitive")));
        FallbackCommentProvider provider = new FallbackCommentProvider(repository, new Random(7));

        LlmComment comment = provider.comment(session(CommentaryStyle.COMPETITIVE), NORMAL_MOVE);

        assertThat(comment.text()).isEqualTo("Сохранённая живая реплика.");
        assertThat(comment.fallback()).isTrue();
        assertThat(comment.model()).isNull();
        assertThat(comment.style()).isEqualTo(CommentaryStyle.COMPETITIVE);
        assertThat(comment.emotion()).isEqualTo(Emotion.CONFIDENT);
        assertThat(comment.liked()).isTrue();
        verify(repository).findByStyleAndLikedTrueAndLlmModelIsNotNull("competitive");
    }

    @Test
    void picksOneOfStoredComments() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        when(repository.findByStyleAndLikedTrueAndLlmModelIsNotNull("neutral")).thenReturn(List.of(
                stored("Первый сохранённый вариант.", "neutral"),
                stored("Второй сохранённый вариант.", "neutral")));
        FallbackCommentProvider provider = new FallbackCommentProvider(repository, new Random(1));

        LlmComment comment = provider.comment(session(CommentaryStyle.NEUTRAL), NORMAL_MOVE);

        assertThat(comment.text()).isIn("Первый сохранённый вариант.", "Второй сохранённый вариант.");
    }

    @Test
    void fallsBackToDeterministicTextWhenNothingStored() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        when(repository.findByStyleAndLikedTrueAndLlmModelIsNotNull("competitive"))
                .thenReturn(List.of());
        FallbackCommentProvider provider = new FallbackCommentProvider(repository, new Random(7));

        LlmComment comment = provider.comment(session(CommentaryStyle.COMPETITIVE), NORMAL_MOVE);

        assertThat(comment.text()).isEqualTo("Мой ход. Интересно, что ты придумаешь.");
        assertThat(comment.fallback()).isTrue();
    }

    @Test
    void repositoryFailureDoesNotBreakComment() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        when(repository.findByStyleAndLikedTrueAndLlmModelIsNotNull("friendly_master"))
                .thenThrow(new IllegalStateException("db down"));
        FallbackCommentProvider provider = new FallbackCommentProvider(repository, new Random(7));

        LlmComment comment = provider.comment(session(CommentaryStyle.FRIENDLY_MASTER), NORMAL_MOVE);

        assertThat(comment.text()).isEqualTo("Делаю осторожный ход, посмотрим на твои планы.");
        assertThat(comment.style()).isEqualTo(CommentaryStyle.FRIENDLY_MASTER);
    }

    @Test
    void withoutRepositoryUsesDeterministicText() {
        FallbackCommentProvider provider = new FallbackCommentProvider();

        LlmComment comment = provider.comment(session(CommentaryStyle.NEUTRAL), NORMAL_MOVE);

        assertThat(comment.text()).isEqualTo("Ход сделан.");
    }
}
