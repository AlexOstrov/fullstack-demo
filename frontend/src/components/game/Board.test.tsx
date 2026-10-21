import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import Board from "./Board";
import type { LegalMoveDto } from "../../api/types";

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

const baseProps = {
  board: initialBoard(),
  legalMoves: [] as LegalMoveDto[],
  currentPlayerColor: "WHITE" as const,
  humanColor: "WHITE" as const,
  selectedCell: null,
  lastMove: null,
  onCellClick: vi.fn(),
  animatingFrom: null,
  animatingTo: null,
  animationType: null,
  interactive: true,
};

describe("Board", () => {
  it("рендерит 64 клетки и начальную расстановку", () => {
    const { container } = render(<Board {...baseProps} />);
    expect(container.querySelectorAll('[role="gridcell"]')).toHaveLength(64);
    expect(container.querySelectorAll(".piece--white")).toHaveLength(12);
    expect(container.querySelectorAll(".piece--black")).toHaveLength(12);
  });

  it("выносит координаты за пределы поля", () => {
    const { container } = render(<Board {...baseProps} />);
    const ranks = container.querySelector(".board__ranks");
    const files = container.querySelector(".board__files");
    expect(ranks?.children).toHaveLength(8);
    expect(ranks?.textContent).toBe("87654321");
    expect(files?.children).toHaveLength(8);
    expect(files?.textContent).toBe("abcdefgh");
  });

  it("переворачивает доску, когда игрок играет чёрными", () => {
    const { container } = render(
      <Board {...baseProps} humanColor="BLACK" currentPlayerColor="BLACK" />,
    );
    expect(container.querySelector(".board__ranks")?.textContent).toBe(
      "12345678",
    );
    expect(container.querySelector(".board__files")?.textContent).toBe(
      "hgfedcba",
    );
    expect(screen.getAllByRole("gridcell")[0]).toHaveAccessibleName(
      /клетка h1/,
    );
  });

  it("передаёт координаты состояния партии при клике по своей шашке", () => {
    const onCellClick = vi.fn();
    render(
      <Board
        {...baseProps}
        humanColor="BLACK"
        currentPlayerColor="BLACK"
        onCellClick={onCellClick}
      />,
    );
    // Нижняя левая клетка перевёрнутой доски — h8 (строка 0, столбец 7):
    // там стоит чёрная шашка игрока.
    fireEvent.click(screen.getByRole("gridcell", { name: /клетка h8/ }));
    expect(onCellClick).toHaveBeenCalledWith(0, 7);
  });

  it("подсвечивает допустимый ход и клетку взятия", () => {
    const legalMoves: LegalMoveDto[] = [
      { path: [{ row: 5, col: 2 }, { row: 4, col: 3 }] },
      { path: [{ row: 5, col: 2 }, { row: 3, col: 4 }] },
    ];
    render(
      <Board
        {...baseProps}
        selectedCell={{ row: 5, col: 2 }}
        legalMoves={legalMoves}
      />,
    );
    expect(screen.getByRole("gridcell", { name: /клетка d4/ })).toHaveClass(
      "board__cell--legal",
    );
    expect(screen.getByRole("gridcell", { name: /клетка e5/ })).toHaveClass(
      "board__cell--capture",
    );
    expect(screen.getByRole("gridcell", { name: /клетка c3/ })).toHaveClass(
      "board__cell--selected",
    );
  });

  it("передаёт клик по допустимой клетке", () => {
    const onCellClick = vi.fn();
    render(
      <Board
        {...baseProps}
        selectedCell={{ row: 5, col: 2 }}
        legalMoves={[{ path: [{ row: 5, col: 2 }, { row: 4, col: 3 }] }]}
        onCellClick={onCellClick}
      />,
    );
    fireEvent.click(screen.getByRole("gridcell", { name: /клетка d4/ }));
    expect(onCellClick).toHaveBeenCalledWith(4, 3);
  });
});
