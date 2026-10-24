package com.example.backend.service;

import com.example.backend.dto.game.TournamentStatsResponse;
import com.example.backend.dto.game.WinnerCount;
import com.example.backend.repository.TournamentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** B1: агрегация статистики побед. */
class TournamentStatsServiceTest {

    private final TournamentRepository repository = mock(TournamentRepository.class);
    private final TournamentStatsService service = new TournamentStatsService(repository);

    @Test
    void aggregatesHumanAiAndDrawWins() {
        when(repository.countByWinner()).thenReturn(List.of(
                new WinnerCount("HUMAN", 4),
                new WinnerCount("AI", 3),
                new WinnerCount("DRAW", 2)));

        TournamentStatsResponse stats = service.stats();

        assertThat(stats.humanWins()).isEqualTo(4);
        assertThat(stats.aiWins()).isEqualTo(3);
        assertThat(stats.draws()).isEqualTo(2);
        assertThat(stats.total()).isEqualTo(9);
    }

    @Test
    void emptyTableReturnsZeros() {
        when(repository.countByWinner()).thenReturn(List.of());

        TournamentStatsResponse stats = service.stats();

        assertThat(stats.humanWins()).isZero();
        assertThat(stats.aiWins()).isZero();
        assertThat(stats.draws()).isZero();
        assertThat(stats.total()).isZero();
    }

    @Test
    void unknownWinnerDoesNotBreakStats() {
        when(repository.countByWinner()).thenReturn(List.of(
                new WinnerCount("HUMAN", 1),
                new WinnerCount("SOMETHING_ELSE", 10)));

        TournamentStatsResponse stats = service.stats();

        assertThat(stats.humanWins()).isEqualTo(1);
        assertThat(stats.total()).isEqualTo(1);
    }
}
