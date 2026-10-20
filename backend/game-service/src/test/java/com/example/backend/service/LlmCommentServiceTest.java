package com.example.backend.service;

import com.example.backend.model.LlmCommentEntity;
import com.example.backend.repository.LlmCommentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCommentServiceTest {

    @Test
    void toggleLikeSetsLikedTrueAndSaves() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentService service = new LlmCommentService(repository);

        UUID gameId = UUID.randomUUID();
        Long commentId = 1L;
        LlmCommentEntity entity = new LlmCommentEntity(
                gameId, "тест", "neutral", "neutral", "qwen2.5:7b-instruct", 1, null, false);
        when(repository.findById(commentId)).thenReturn(Optional.of(entity));

        boolean result = service.toggleLike(gameId, commentId, true);

        assertThat(result).isTrue();
        assertThat(entity.isLiked()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    void toggleLikeSetsLikedFalseAndDeletes() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentService service = new LlmCommentService(repository);

        UUID gameId = UUID.randomUUID();
        Long commentId = 1L;
        LlmCommentEntity entity = new LlmCommentEntity(
                gameId, "тест", "neutral", "neutral", "qwen2.5:7b-instruct", 1, null, true);
        when(repository.findById(commentId)).thenReturn(Optional.of(entity));

        boolean result = service.toggleLike(gameId, commentId, false);

        assertThat(result).isTrue();
        verify(repository).delete(entity);
    }

    @Test
    void toggleLikeReturnsFalseWhenCommentNotFound() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentService service = new LlmCommentService(repository);

        when(repository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.toggleLike(UUID.randomUUID(), 999L, true);

        assertThat(result).isFalse();
    }

    @Test
    void toggleLikeReturnsFalseWhenWrongGameId() {
        LlmCommentRepository repository = mock(LlmCommentRepository.class);
        LlmCommentService service = new LlmCommentService(repository);

        UUID gameId = UUID.randomUUID();
        UUID otherGameId = UUID.randomUUID();
        Long commentId = 1L;
        LlmCommentEntity entity = new LlmCommentEntity(
                otherGameId, "тест", "neutral", "neutral", "qwen2.5:7b-instruct", 1, null, true);
        when(repository.findById(commentId)).thenReturn(Optional.of(entity));

        boolean result = service.toggleLike(gameId, commentId, true);

        assertThat(result).isFalse();
        verify(repository, org.mockito.Mockito.never()).save(any());
        verify(repository, org.mockito.Mockito.never()).delete(any());
    }
}
