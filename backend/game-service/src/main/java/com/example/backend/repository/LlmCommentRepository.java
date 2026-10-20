package com.example.backend.repository;

import com.example.backend.model.LlmCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Доступ к сохранённым комментариям LLM (таблица {@code llm_comments}). */
@Repository
public interface LlmCommentRepository extends JpaRepository<LlmCommentEntity, Long> {

    List<LlmCommentEntity> findByGameId(UUID gameId);

    void deleteByGameId(UUID gameId);
}
