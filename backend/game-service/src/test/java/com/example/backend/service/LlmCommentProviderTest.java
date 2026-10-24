package com.example.backend.service;

import com.example.backend.config.LlmProperties;
import com.example.backend.dto.game.CommentaryStyle;
import com.example.backend.dto.game.Difficulty;
import com.example.backend.dto.game.Emotion;
import com.example.backend.dto.game.PlayerColor;
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
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCommentProviderTest {

    private static GameSession session(Color aiColor, CommentaryStyle style, String llmModel) {
        GameState state = GameState.newGame(aiColor.opposite());
        GamePreferences prefs = new GamePreferences(
                PlayerColor.WHITE, Difficulty.EASY, null, null, style, llmModel);
        AiSettings aiSettings = new AiSettings(4, 1500, 65536);
        return new GameSession(state, prefs, aiSettings, Instant.now());
    }

    private static LlmProperties llmProperties(boolean enabled, String model, long timeoutMs) {
        return new LlmProperties(
                "http://localhost:8081",
                model,
                List.of(model),
                timeoutMs,
                1,
                0.7,
                2048,
                enabled
        );
    }

    @Test
    void llmEnabledSuccessReturnsLlmCommentWithStyleAndEmotion() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("Отличный ход, дамка в игре!");
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession session = session(Color.BLACK, CommentaryStyle.COMPETITIVE, "qwen2.5:7b-instruct");
        Move aiMove = new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.PROMOTION);
        LlmComment comment = provider.comment(session, aiMove);

        assertThat(comment.fallback()).isFalse();
        assertThat(comment.model()).isEqualTo("qwen2.5:7b-instruct");
        assertThat(comment.text()).isEqualTo("Отличный ход, дамка в игре!");
        assertThat(comment.moveNumber()).isEqualTo(session.state().moveNumber());
        assertThat(comment.style()).isEqualTo(CommentaryStyle.COMPETITIVE);
        assertThat(comment.emotion()).isEqualTo(Emotion.CONFIDENT);
        assertThat(comment.liked()).isTrue();
        verify(client).chat(anyString(), anyString());
    }

    @Test
    void llmUnavailableReturnsFallbackWithStyleAndEmotion() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString()))
                .thenThrow(new LlmServiceClient.LlmServiceUnavailable("connection refused", null));
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession session = session(Color.BLACK, CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct");
        Move aiMove = new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL);
        LlmComment comment = provider.comment(session, aiMove);

        assertThat(comment.fallback()).isTrue();
        assertThat(comment.model()).isNull();
        assertThat(comment.text()).isEqualTo("Ход сделан.");
        assertThat(comment.style()).isEqualTo(CommentaryStyle.NEUTRAL);
        assertThat(comment.emotion()).isEqualTo(Emotion.NEUTRAL);
        assertThat(comment.liked()).isTrue();
        verify(client).chat(anyString(), anyString());
    }

    @Test
    void llmEmptyResponseReturnsFallbackWithStyleAndEmotion() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn(null);
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession session = session(Color.WHITE, CommentaryStyle.FRIENDLY_MASTER, "qwen2.5:7b-instruct");
        Move aiMove = new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.CAPTURE);
        LlmComment comment = provider.comment(session, aiMove);

        assertThat(comment.fallback()).isTrue();
        assertThat(comment.model()).isNull();
        assertThat(comment.text()).contains("взятием");
        assertThat(comment.style()).isEqualTo(CommentaryStyle.FRIENDLY_MASTER);
        assertThat(comment.emotion()).isEqualTo(Emotion.NEUTRAL);
        assertThat(comment.liked()).isTrue();
        verify(client).chat(anyString(), anyString());
    }

    @Test
    void llmDisabledReturnsFallbackWithoutCallWithStyleAndEmotion() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(false, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession session = session(Color.BLACK, CommentaryStyle.COMPETITIVE, "qwen2.5:7b-instruct");
        Move aiMove = new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.PROMOTION);
        LlmComment comment = provider.comment(session, aiMove);

        assertThat(comment.fallback()).isTrue();
        assertThat(comment.model()).isNull();
        assertThat(comment.text()).isEqualTo("А вот и дамка. Придётся тебе попотеть.");
        assertThat(comment.style()).isEqualTo(CommentaryStyle.COMPETITIVE);
        assertThat(comment.emotion()).isEqualTo(Emotion.CONFIDENT);
        assertThat(comment.liked()).isTrue();
        verify(client, never()).chat(anyString(), anyString());
    }

    @Test
    void promptContainsBoardAndMoveInfoAndStyle() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("комментарий");
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession session = session(Color.WHITE, CommentaryStyle.FRIENDLY_MASTER, "qwen2.5:7b-instruct");
        Move aiMove = new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.PROMOTION);
        provider.comment(session, aiMove);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client).chat(captor.capture(), org.mockito.ArgumentMatchers.eq("qwen2.5:7b-instruct"));
        String prompt = captor.getValue();
        assertThat(prompt).contains("комментатор");
        assertThat(prompt).contains("friendly_master");
        assertThat(prompt).isNotBlank();
    }

    @Test
    void emotionOfMapsStyleToEmotion() {
        FallbackCommentProvider fallback = new FallbackCommentProvider();

        // Прямой доступ к приватному методу через рефлексию не требуется —
        // проверяем через行为: комментарий с разными стилями должен иметь разную эмоцию
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("комментарий");
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        GameSession friendlySession = session(Color.WHITE, CommentaryStyle.FRIENDLY_MASTER, "qwen2.5:7b-instruct");
        LlmComment friendlyComment = provider.comment(friendlySession,
                new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));
        assertThat(friendlyComment.emotion()).isEqualTo(Emotion.NEUTRAL);

        GameSession competitiveSession = session(Color.WHITE, CommentaryStyle.COMPETITIVE, "qwen2.5:7b-instruct");
        LlmComment competitiveComment = provider.comment(competitiveSession,
                new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));
        assertThat(competitiveComment.emotion()).isEqualTo(Emotion.CONFIDENT);

        GameSession neutralSession = session(Color.WHITE, CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct");
        LlmComment neutralComment = provider.comment(neutralSession,
                new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));
        assertThat(neutralComment.emotion()).isEqualTo(Emotion.NEUTRAL);
        assertThat(neutralComment.liked()).isTrue();
    }

    @Test
    void likedDefaultsToTrueForAllStyles() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("комментарий");
        FallbackCommentProvider fallback = new FallbackCommentProvider();
        LlmProperties props = llmProperties(true, "qwen2.5:7b-instruct", 8000);
        LlmCommentProvider provider = new LlmCommentProvider(client, props, fallback);

        for (CommentaryStyle style : CommentaryStyle.values()) {
            GameSession s = session(Color.WHITE, style, "qwen2.5:7b-instruct");
            LlmComment c = provider.comment(s,
                    new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));
            assertThat(c.liked()).as("liked for " + style).isTrue();
        }
    }

    @Test
    void foreignScriptResponseIsRejectedAndStoredCommentUsed() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString()))
                .thenReturn("Отличный ход! 但是 это уже не по-русски.");
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        when(repository.findByStyleAndLikedTrueAndLlmModelIsNotNull("competitive"))
                .thenReturn(List.of(new LlmCommentEntity(
                        UUID.randomUUID(), "Сохранённая реплика из прошлой партии.",
                        "competitive", "confident", "qwen2.5:7b-instruct", 7, Instant.now())));
        FallbackCommentProvider fallback = new FallbackCommentProvider(repository, new Random(3));
        LlmCommentProvider provider = new LlmCommentProvider(
                client, llmProperties(true, "qwen2.5:7b-instruct", 8000), fallback);

        LlmComment comment = provider.comment(
                session(Color.BLACK, CommentaryStyle.COMPETITIVE, "qwen2.5:7b-instruct"),
                new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));

        assertThat(comment.fallback()).isTrue();
        assertThat(comment.model()).isNull();
        assertThat(comment.text()).isEqualTo("Сохранённая реплика из прошлой партии.");
    }

    @Test
    void sanitizesQuotedMarkdownResponse() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("«**Отличный ход!**»");
        LlmCommentProvider provider = new LlmCommentProvider(
                client, llmProperties(true, "qwen2.5:7b-instruct", 8000),
                new FallbackCommentProvider());

        LlmComment comment = provider.comment(
                session(Color.WHITE, CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct"),
                new Move(List.of(new Cell(6, 4), new Cell(4, 4)), MoveType.NORMAL));

        assertThat(comment.fallback()).isFalse();
        assertThat(comment.text()).isEqualTo("Отличный ход!");
    }

    @Test
    void promptUsesAlgebraicNotationAndRequiresRussian() {
        LlmServiceClient client = mock(LlmServiceClient.class);
        when(client.chat(anyString(), anyString())).thenReturn("комментарий");
        LlmCommentProvider provider = new LlmCommentProvider(
                client, llmProperties(true, "qwen2.5:7b-instruct", 8000),
                new FallbackCommentProvider());

        Move move = new Move(List.of(new Cell(6, 4), new Cell(5, 3)), MoveType.NORMAL);
        provider.comment(session(Color.WHITE, CommentaryStyle.NEUTRAL, "qwen2.5:7b-instruct"), move);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client).chat(captor.capture(), org.mockito.ArgumentMatchers.eq("qwen2.5:7b-instruct"));
        String prompt = captor.getValue();
        assertThat(prompt).contains("ТОЛЬКО по-русски");
        assertThat(prompt).contains("e2–d3");
        assertThat(prompt).contains("c3–d4");
        assertThat(prompt).contains("c3×e5");
        assertThat(prompt).contains("не пиши «строка», «столбец»");
        assertThat(prompt).contains("a b c d e f g h");
    }

    @Test
    void moveNotationUsesCrossForCaptureAndDashForQuietMove() {
        assertThat(LlmCommentProvider.moveNotation(
                new Move(List.of(new Cell(2, 1), new Cell(4, 3)), MoveType.CAPTURE)))
                .isEqualTo("b6×d4");
        assertThat(LlmCommentProvider.moveNotation(
                new Move(List.of(new Cell(1, 0), new Cell(0, 1)), MoveType.PROMOTION)))
                .isEqualTo("a7–b8");
    }
}
