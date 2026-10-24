package com.example.backend.repository;

import com.example.backend.dto.game.WinnerCount;
import com.example.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Доступ к результатам завершённых партий (ТЗ 4.7). */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findByGameId(UUID gameId);

    /** Агрегат по победителю для блока статистики (B1). */
    @Query("select new com.example.backend.dto.game.WinnerCount(t.winner, count(t)) "
            + "from Tournament t group by t.winner")
    List<WinnerCount> countByWinner();

    void deleteByGameId(UUID gameId);
}
