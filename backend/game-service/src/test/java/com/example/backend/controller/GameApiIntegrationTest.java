package com.example.backend.controller;

import com.example.backend.model.Tournament;
import com.example.backend.repository.TournamentRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты API (ТЗ 12, Sprint 3): жизненный цикл партии,
 * идемпотентность, коды ошибок и автосохранение результата в {@code tournament}.
 * <p>Против реальной БД (ADR-0008); транзакция теста откатывается — таблица
 * {@code tournament} после прогона не засоряется.
 */
@SpringBootTest(properties = "llm.enabled=false")
@AutoConfigureMockMvc
@Transactional
class GameApiIntegrationTest {

    private static final String DEFAULT_MODEL = "qwen2.5:7b-instruct";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Test
    void startReturnsInitialState() throws Exception {
        String gameId = startGame("WHITE", "EASY", null, null);

        mockMvc.perform(get("/api/game/{id}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.humanColor").value("WHITE"))
                .andExpect(jsonPath("$.aiColor").value("BLACK"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"))
                .andExpect(jsonPath("$.moveNumber").value(0))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.board.length()").value(8))
                .andExpect(jsonPath("$.board[0].length()").value(8))
                .andExpect(jsonPath("$.board[0][1]").value("b"))
                .andExpect(jsonPath("$.board[7][0]").value("w"))
                .andExpect(jsonPath("$.forcedCapture").value(false))
                .andExpect(jsonPath("$.endReason").doesNotExist())
                .andExpect(jsonPath("$.ai").doesNotExist())
                .andExpect(jsonPath("$.legalMoves.length()").value(7));
    }

    @Test
    void moveThenAiMoveFullCycle() throws Exception {
        String gameId = startGame("WHITE", "CUSTOM", 1, 200);

        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody(0, new int[][]{{5, 0}, {4, 1}})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moveNumber").value(1))
                .andExpect(jsonPath("$.currentPlayer").value("BLACK"))
                .andExpect(jsonPath("$.lastMove.type").value("NORMAL"))
                .andExpect(jsonPath("$.ai").doesNotExist());

        mockMvc.perform(post("/api/game/{id}/ai-move", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moveNumber").value(2))
                .andExpect(jsonPath("$.currentPlayer").value("WHITE"))
                .andExpect(jsonPath("$.ai.move.path.length()").value(2))
                .andExpect(jsonPath("$.ai.commentSource").value("FALLBACK"))
                .andExpect(jsonPath("$.ai.comment").isNotEmpty());

        mockMvc.perform(get("/api/game/{id}/moves", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moves.length()").value(2))
                .andExpect(jsonPath("$.moves[0].moveNumber").value(1))
                .andExpect(jsonPath("$.moves[0].player").value("WHITE"))
                .andExpect(jsonPath("$.moves[1].player").value("BLACK"))
                .andExpect(jsonPath("$.comments.length()").value(1));
    }

    @Test
    void staleClientMoveNumberGivesStaleState() throws Exception {
        String gameId = startGame("WHITE", "EASY", null, null);

        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody(4, new int[][]{{5, 0}, {4, 1}})))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_STATE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/game/" + gameId + "/move"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void moveOnAiTurnGivesNotYourTurn() throws Exception {
        String gameId = startGame("BLACK", "EASY", null, null);

        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody(0, new int[][]{{5, 0}, {4, 1}})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_YOUR_TURN"));
    }

    @Test
    void illegalMoveGivesInvalidMove() throws Exception {
        String gameId = startGame("WHITE", "EASY", null, null);

        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody(0, new int[][]{{5, 0}, {3, 0}})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MOVE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void validationErrorsAreReported() throws Exception {
        mockMvc.perform(post("/api/game/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String gameId = startGame("WHITE", "EASY", null, null);
        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":[],\"clientMoveNumber\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/game/{id}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":[{\"row\":9,\"col\":0}],\"clientMoveNumber\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownGameGivesGameNotFound() throws Exception {
        mockMvc.perform(get("/api/game/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    @Test
    void llmModelsEndpointExposesAllowlist() throws Exception {
        mockMvc.perform(get("/api/config/llm-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.models.length()").value(4))
                .andExpect(jsonPath("$.defaultModel").value(DEFAULT_MODEL));
    }

    @Test
    void resignPersistsTournamentResult() throws Exception {
        String gameId = startGame("WHITE", "MEDIUM", null, null);

        mockMvc.perform(post("/api/game/{id}/resign", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLACK_WON"))
                .andExpect(jsonPath("$.endReason").value("RESIGN"));

        mockMvc.perform(post("/api/game/{id}/resign", gameId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));

        Tournament saved = savedTournament(gameId);
        assertEquals("AI", saved.getWinner());
        assertEquals("BLACK", saved.getWinnerColor());
        assertEquals("WHITE", saved.getHumanColor());
        assertEquals("RESIGN", saved.getEndReason());
        assertEquals(0, saved.getMovesCount());
        assertTrue(saved.getDurationSeconds() >= 0);
        assertNotNull(saved.getFinishedAt());
    }

    @Test
    void drawAcceptsAndPersistsDraw() throws Exception {
        String gameId = startGame("WHITE", "EASY", null, null);

        mockMvc.perform(post("/api/game/{id}/draw", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.game.status").value("DRAW"))
                .andExpect(jsonPath("$.game.endReason").value("DRAW_AGREEMENT"));

        Tournament saved = tournamentRepository.findByGameId(UUID.fromString(gameId)).orElseThrow();
        assertEquals("DRAW", saved.getWinner());
        assertNull(saved.getWinnerColor());
    }

    private Tournament savedTournament(String gameId) {
        return tournamentRepository.findByGameId(UUID.fromString(gameId))
                .orElseThrow(() -> new AssertionError("Результат партии не сохранён"));
    }

    private String startGame(String color, String difficulty, Integer depth, Integer budget)
            throws Exception {
        String body = """
                {"playerColor":"%s","difficulty":"%s","aiDepth":%s,"aiTimeBudgetMs":%s,
                 "commentaryStyle":"FRIENDLY_MASTER","llmModel":"%s"}"""
                .formatted(color, difficulty, depth, budget, DEFAULT_MODEL);
        String response = mockMvc.perform(post("/api/game/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(response, "$.gameId");
    }

    private static String moveBody(int clientMoveNumber, int[][] path) {
        StringBuilder cells = new StringBuilder();
        for (int[] cell : path) {
            if (!cells.isEmpty()) {
                cells.append(',');
            }
            cells.append("{\"row\":").append(cell[0]).append(",\"col\":").append(cell[1]).append('}');
        }
        return "{\"path\":[" + cells + "],\"clientMoveNumber\":" + clientMoveNumber + '}';
    }
}
