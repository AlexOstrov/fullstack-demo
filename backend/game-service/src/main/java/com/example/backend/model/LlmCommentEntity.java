package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Сохранённый комментарий LLM к ходу ИИ (таблица {@code llm_comments}, миграция V4).
 * <p>Запись создаётся автоматически при генерации комментария ИИ (best-effort:
 * сбой БД логируется и не ломает партию, см. ADR-0004).
 */
@Entity
@Table(name = "llm_comments")
public class LlmCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "style", nullable = false, length = 32)
    private String style;

    @Column(name = "emotion", length = 32)
    private String emotion;

    @Column(name = "llm_model", length = 128)
    private String llmModel;

    @Column(name = "move_number")
    private Integer moveNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "liked", nullable = false)
    private boolean liked = true;

    protected LlmCommentEntity() {
    }

    public LlmCommentEntity(UUID gameId, String comment, String style, String emotion,
                            String llmModel, Integer moveNumber, Instant createdAt) {
        this(gameId, comment, style, emotion, llmModel, moveNumber, createdAt, true);
    }

    public LlmCommentEntity(UUID gameId, String comment, String style, String emotion,
                            String llmModel, Integer moveNumber, Instant createdAt, boolean liked) {
        this.gameId = gameId;
        this.comment = comment;
        this.style = style;
        this.emotion = emotion;
        this.llmModel = llmModel;
        this.moveNumber = moveNumber;
        this.createdAt = createdAt;
        this.liked = liked;
    }

    public Long getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public String getComment() {
        return comment;
    }

    public String getStyle() {
        return style;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public Integer getMoveNumber() {
        return moveNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}
