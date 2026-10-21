import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import MoveHistory from "./MoveHistory";
import type { MoveRecordDto } from "../../api/types";

const moves: MoveRecordDto[] = [
  {
    moveNumber: 1,
    player: "WHITE",
    type: "NORMAL",
    path: [
      { row: 5, col: 2 },
      { row: 4, col: 3 },
    ],
  },
  {
    moveNumber: 2,
    player: "BLACK",
    type: "CAPTURE",
    path: [
      { row: 2, col: 1 },
      { row: 4, col: 3 },
    ],
  },
  {
    moveNumber: 3,
    player: "WHITE",
    type: "PROMOTION",
    path: [
      { row: 1, col: 0 },
      { row: 0, col: 1 },
    ],
  },
];

describe("MoveHistory", () => {
  it("показывает ходы в нотации с разделителями", () => {
    render(<MoveHistory moves={moves} />);
    expect(screen.getByText("c3–d4")).toBeInTheDocument();
    expect(screen.getByText("b6×d4")).toBeInTheDocument();
    expect(screen.getByText("3 хода")).toBeInTheDocument();
  });

  it("помечает превращение в дамку", () => {
    render(<MoveHistory moves={moves} />);
    expect(screen.getByText("дамка")).toBeInTheDocument();
  });

  it("показывает взятие с превращением через ×", () => {
    render(
      <MoveHistory
        moves={[
          {
            moveNumber: 1,
            player: "BLACK",
            type: "PROMOTION",
            path: [
              { row: 3, col: 2 },
              { row: 1, col: 0 },
            ],
          },
        ]}
      />,
    );
    expect(screen.getByText("c5×a7")).toBeInTheDocument();
    expect(screen.getByText("дамка")).toBeInTheDocument();
  });

  it("показывает пустое состояние", () => {
    render(<MoveHistory moves={[]} />);
    expect(
      screen.getByText("Ходы появятся здесь после начала партии."),
    ).toBeInTheDocument();
  });
});
