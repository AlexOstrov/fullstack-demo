package com.example.backend.controller;

import com.example.backend.dto.game.LikedRequest;
import com.example.backend.error.ApiErrorCode;
import com.example.backend.error.ApiException;
import com.example.backend.service.LlmCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Лайки комментариев LLM (ТЗ 7, B2).
 * Базовый путь — {@code /api/game} (ADR-0002).
 */
@RestController
@RequestMapping("/api/game")
public class LlmCommentController {

    private final LlmCommentService llmCommentService;

    public LlmCommentController(LlmCommentService llmCommentService) {
        this.llmCommentService = llmCommentService;
    }

    /**
     * Поставить или снять лайк с комментария (ТЗ 7, B2).
     * {@code liked=true} → комментарий остаётся в {@code llm_comments};
     * {@code liked=false} → комментарий удаляется из {@code llm_comments}.
     */
    @PostMapping("/{id}/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable UUID id,
            @PathVariable Long commentId,
            @Valid @RequestBody LikedRequest request) {
        boolean found = llmCommentService.toggleLike(id, commentId, request.liked());
        if (!found) {
            throw new ApiException(ApiErrorCode.GAME_NOT_FOUND,
                    "Комментарий не найден в партии: commentId=" + commentId);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
