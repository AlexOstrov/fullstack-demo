import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import Board from "./Board";

describe("Board (заглушка Sprint 0)", () => {
  it("рендерит доску 8×8 — ровно 64 клетки", () => {
    const { container } = render(<Board />);
    expect(container.querySelectorAll('[role="gridcell"]')).toHaveLength(64);
  });

  it("расставляет 12 белых и 12 чёрных шашек", () => {
    const { container } = render(<Board />);
    expect(container.querySelectorAll(".piece--white")).toHaveLength(12);
    expect(container.querySelectorAll(".piece--black")).toHaveLength(12);
  });

  it("ставит шашки только на тёмных клетках ((row + col) нечётно)", () => {
    const { container } = render(<Board />);
    const pieces = container.querySelectorAll(".piece");
    expect(pieces.length).toBeGreaterThan(0);
    pieces.forEach((piece) => {
      expect(
        piece.parentElement?.classList.contains("board__cell--dark"),
      ).toBe(true);
    });
  });

  it("ориентирует доску под игрока: белые снизу (ряды 5–7), чёрные сверху (ряды 0–2)", () => {
    const { container } = render(<Board />);
    const cells = Array.from(container.querySelectorAll(".board__cell"));

    cells.forEach((cell, index) => {
      if (cell.querySelector(".piece--white")) {
        expect(index).toBeGreaterThanOrEqual(40); // ряд 5+: 5 * 8 = 40
      }
      if (cell.querySelector(".piece--black")) {
        expect(index).toBeLessThan(24); // ряды 0–2: 3 * 8 = 24
      }
    });
  });

  it("клетки имеют подписи полей a1–h8 без дублей", () => {
    const { container } = render(<Board />);
    const labels = Array.from(
      container.querySelectorAll('[role="gridcell"]'),
    ).map((cell) => cell.getAttribute("aria-label")?.split(",")[0]);
    expect(new Set(labels).size).toBe(64);
    expect(labels).toContain("клетка a1");
    expect(labels).toContain("клетка h8");
  });
});
