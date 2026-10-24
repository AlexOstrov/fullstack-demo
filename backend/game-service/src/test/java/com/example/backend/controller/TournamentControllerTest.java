package com.example.backend.controller;

import com.example.backend.dto.game.TournamentStatsResponse;
import com.example.backend.service.TournamentStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B1: контракт {@code GET /api/tournament}. */
class TournamentControllerTest {

    @Test
    void returnsStats() throws Exception {
        TournamentStatsService service = mock(TournamentStatsService.class);
        when(service.stats()).thenReturn(new TournamentStatsResponse(3, 2, 1, 6));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TournamentController(service)).build();

        mvc.perform(get("/api/tournament"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humanWins").value(3))
                .andExpect(jsonPath("$.aiWins").value(2))
                .andExpect(jsonPath("$.draws").value(1))
                .andExpect(jsonPath("$.total").value(6));
    }
}
