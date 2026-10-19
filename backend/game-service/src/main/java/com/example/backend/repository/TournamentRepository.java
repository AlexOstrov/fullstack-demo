package com.example.backend.repository;

import com.example.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Доступ к результатам завершённых партий (ТЗ 4.7). */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findByGameId(UUID gameId);

    void deleteByGameId(UUID gameId);
}
