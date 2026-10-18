package com.example.backend.controller;

import com.example.backend.dto.game.DrawResponse;
import com.example.backend.dto.game.GameStateResponse;
import com.example.backend.dto.game.MoveRequest;
import com.example.backend.dto.game.MovesResponse;
import com.example.backend.dto.game.StartGameRequest;
import com.example.backend.service.AiMoveOutcome;
import com.example.backend.service.DrawOutcome;
import com.example.backend.service.GameResponseMapper;
import com.example.backend.service.GameService;
import com.example.backend.session.GameSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Жизненный цикл партии (ТЗ 5). Базовый путь — {@code /api/game} (ADR-0002).
 * Ошибки — в едином формате (ТЗ 5.3) через {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final GameResponseMapper mapper;

    public GameController(GameService gameService, GameResponseMapper mapper) {
        this.gameService = gameService;
        this.mapper = mapper;
    }

    /** Создать партию с настройками (цвет, сложность, стиль, модель). */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public GameStateResponse start(@Valid @RequestBody StartGameRequest request) {
        return mapper.toResponse(gameService.start(request));
    }

    /** Текущее состояние партии. */
    @GetMapping("/{id}")
    public GameStateResponse get(@PathVariable UUID id) {
        return mapper.toResponse(gameService.get(id));
    }

    /** История ходов и комментариев. */
    @GetMapping("/{id}/moves")
    public MovesResponse moves(@PathVariable UUID id) {
        return mapper.toMoves(gameService.get(id));
    }

    /** Ход игрока. */
    @PostMapping("/{id}/move")
    public GameStateResponse move(@PathVariable UUID id,
                                  @Valid @RequestBody MoveRequest request) {
        return mapper.toResponse(gameService.humanMove(id, request));
    }

    /** Ход ИИ: минимакс → ход → комментарий. */
    @PostMapping("/{id}/ai-move")
    public GameStateResponse aiMove(@PathVariable UUID id) {
        AiMoveOutcome outcome = gameService.aiMove(id);
        GameSession session = outcome.session();
        return mapper.toResponse(session,
                mapper.toAiTurn(session.state().lastMove(), outcome.comment()));
    }

    /** Сдаться. */
    @PostMapping("/{id}/resign")
    public GameStateResponse resign(@PathVariable UUID id) {
        return mapper.toResponse(gameService.resign(id));
    }

    /** Предложить ничью — решает ИИ. */
    @PostMapping("/{id}/draw")
    public DrawResponse draw(@PathVariable UUID id) {
        DrawOutcome outcome = gameService.draw(id);
        return new DrawResponse(outcome.accepted(), mapper.toResponse(outcome.session()));
    }

    /** Начать партию заново; без тела — с прежними настройками. */
    @PostMapping("/{id}/restart")
    public GameStateResponse restart(@PathVariable UUID id,
                                     @Valid @RequestBody(required = false) StartGameRequest request) {
        return mapper.toResponse(gameService.restart(id, request));
    }
}
