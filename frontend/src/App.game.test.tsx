import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "./App";
import type { GameStateResponse } from "./api/types";

vi.mock("./api/client", () => ({
  startGame: vi.fn(),
  getGame: vi.fn(),
  getMoves: vi.fn(),
  makeMove: vi.fn(),
  makeAiMove: vi.fn(),
  resign: vi.fn(),
  offerDraw: vi.fn(),
  restartGame: vi.fn(),
  getLlmModels: vi.fn(),
  getTournamentStats: vi.fn(),
  likeComment: vi.fn(),
}));

import * as client from "./api/client";

/** Начальная расстановка в токенах бэкенда. */
function initialBoard(): string[][] {
  const board = Array.from({ length: 8 }, () =>
    Array.from({ length: 8 }, () => ""),
  );
  for (let row = 0; row < 3; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) board[row][col] = "b";
    }
  }
  for (let row = 5; row < 8; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) board[row][col] = "w";
    }
  }
  return board;
}

const gameState: GameStateResponse = {
  gameId: "game-1",
  humanColor: "WHITE",
  aiColor: "BLACK",
  status: "IN_PROGRESS",
  currentPlayer: "WHITE",
  moveNumber: 1,
  version: 2,
  board: initialBoard(),
  lastMove: null,
  legalMoves: [{ path: [{ row: 5, col: 2 }, { row: 4, col: 3 }] }],
  forcedCapture: false,
  endReason: null,
  ai: null,
};

describe("App: игровой цикл", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(client.getLlmModels).mockResolvedValue({
      models: ["qwen2.5:7b-instruct"],
      defaultModel: "qwen2.5:7b-instruct",
    });
    vi.mocked(client.getMoves).mockResolvedValue({ moves: [], comments: [] });
    vi.mocked(client.getTournamentStats).mockResolvedValue({
      humanWins: 0,
      aiWins: 0,
      draws: 0,
      total: 0,
    });
    vi.mocked(client.startGame).mockResolvedValue(gameState);
  });

  it("выбирает простую шашку и подсвечивает её ходы", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: /Начать партию/ }));
    await screen.findByRole("button", { name: /Начать новую партию/ });

    const piece = screen.getByRole("gridcell", {
      name: /клетка c3, белая шашка/,
    });
    fireEvent.click(piece);

    await waitFor(() =>
      expect(
        screen.getByRole("gridcell", { name: /клетка c3/ }),
      ).toHaveClass("board__cell--selected"),
    );
    expect(screen.getByRole("gridcell", { name: /клетка d4/ })).toHaveClass(
      "board__cell--legal",
    );
  });

  it("снимает выбор при повторном клике по шашке", async () => {
    render(<App />);

    fireEvent.click(screen.getByRole("button", { name: /Начать партию/ }));
    await screen.findByRole("button", { name: /Начать новую партию/ });

    const piece = () => screen.getByRole("gridcell", { name: /клетка c3/ });

    fireEvent.click(piece());
    await waitFor(() => expect(piece()).toHaveClass("board__cell--selected"));

    fireEvent.click(piece());
    await waitFor(() =>
      expect(piece()).not.toHaveClass("board__cell--selected"),
    );
  });

  it("обновляет статистику после завершения партии", async () => {
    vi.mocked(client.startGame).mockResolvedValue({
      ...gameState,
      status: "WHITE_WON",
      endReason: "RESIGN",
    });

    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: /Начать партию/ }));
    await screen.findByRole("button", { name: /Начать новую партию/ });

    await waitFor(() =>
      expect(client.getTournamentStats).toHaveBeenCalledTimes(2),
    );
  });
});
