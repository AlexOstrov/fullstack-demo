type Cell = "EMPTY" | "WHITE_MAN" | "BLACK_MAN" | "WHITE_KING" | "BLACK_KING";

type ColoredCell = Exclude<Cell, "EMPTY">;

/** Начальная позиция: чёрные — сверху (ряды 0–2), белые — снизу (ряды 5–7). */
function createInitialBoard(): Cell[][] {
  const board: Cell[][] = Array.from({ length: 8 }, () =>
    Array.from({ length: 8 }, () => "EMPTY" as Cell),
  );

  for (let row = 0; row < 3; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) {
        board[row][col] = "BLACK_MAN";
      }
    }
  }

  for (let row = 5; row < 8; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) {
        board[row][col] = "WHITE_MAN";
      }
    }
  }

  return board;
}

const PIECE_CLASS: Record<ColoredCell, string> = {
  WHITE_MAN: "piece piece--white",
  BLACK_MAN: "piece piece--black",
  WHITE_KING: "piece piece--white piece--king",
  BLACK_KING: "piece piece--black piece--king",
};

const PIECE_LABEL: Record<ColoredCell, string> = {
  WHITE_MAN: "белая шашка",
  BLACK_MAN: "чёрная шашка",
  WHITE_KING: "белая дамка",
  BLACK_KING: "чёрная дамка",
};

const COLUMN_LABELS = ["a", "b", "c", "d", "e", "f", "g", "h"];
const INITIAL_BOARD = createInitialBoard();

/**
 * Заглушка доски 8×8 (Sprint 0).
 * Игрок играет белыми — его шашки снизу. Интерактив появится в Sprint 1.
 */
export default function Board() {
  return (
    <div className="board" role="grid" aria-label="Игровая доска 8×8, заглушка">
      {INITIAL_BOARD.map((row, rowIndex) =>
        row.map((cell, colIndex) => {
          const isDark = (rowIndex + colIndex) % 2 === 1;
          const label = `${COLUMN_LABELS[colIndex]}${8 - rowIndex}`;

          return (
            <div
              key={label}
              role="gridcell"
              aria-label={`клетка ${label}${cell !== "EMPTY" ? `, ${PIECE_LABEL[cell]}` : ""}`}
              className={`board__cell board__cell--${isDark ? "dark" : "light"}`}
            >
              {cell !== "EMPTY" && (
                <span className={PIECE_CLASS[cell]} aria-hidden="true" />
              )}
            </div>
          );
        }),
      )}
    </div>
  );
}
