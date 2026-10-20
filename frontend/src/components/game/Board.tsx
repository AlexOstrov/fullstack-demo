/**
 * Игровая доска 8×8. Рендерится из состояния бэкенда (GameStateResponse).
 * Поддерживает выбор шашки, подсветку допустимых ходов и клик для хода.
 */

import { useCallback } from "react";
import type { LegalMoveDto } from "../../api/types";
import Cell from "./Cell";

interface BoardProps {
  board: string[][]; // компактные токены: "", "w", "W", "b", "B"
  legalMoves: LegalMoveDto[];
  currentPlayerColor: "WHITE" | "BLACK";
  humanColor: "WHITE" | "BLACK";
  selectedCell: { row: number; col: number } | null;
  onCellClick: (row: number, col: number) => void;
}

const TOKEN_TO_PIECE: Record<string, string | null> = {
  "": null,
  w: "WHITE_MAN",
  W: "WHITE_KING",
  b: "BLACK_MAN",
  B: "BLACK_KING",
};

const COLUMN_LABELS = ["a", "b", "c", "d", "e", "f", "g", "h"];

export default function Board({
  board,
  legalMoves,
  currentPlayerColor,
  humanColor,
  selectedCell,
  onCellClick,
}: BoardProps) {
  const handleCellClick = useCallback(
    (row: number, col: number) => {
      onCellClick(row, col);
    },
    [onCellClick],
  );

  const isLegalMove = useCallback(
    (row: number, col: number) => {
      return legalMoves.some((move) => {
        const last = move.path[move.path.length - 1];
        return last.row === row && last.col === col;
      });
    },
    [legalMoves],
  );

  const isCaptureCell = useCallback(
    (row: number, col: number) => {
      return legalMoves.some((move) => {
        const last = move.path[move.path.length - 1];
        return last.row === row && last.col === col && move.path.length > 2;
      });
    },
    [legalMoves],
  );

  return (
    <div
      className="board"
      role="grid"
      aria-label={`Игровая доска 8×8, игрок ${humanColor}`}
    >
      {/* Номера колонок (верх) */}
      <div className="board__labels board__labels--top">
        {COLUMN_LABELS.map((c) => (
          <div key={c} className="board__label">
            {c}
          </div>
        ))}
      </div>

      <div className="board__grid">
        {board.map((row, rowIndex) => (
          <div key={rowIndex} style={{ display: "contents" }}>
            {/* Номер строки (слева) */}
            <div className="board__label board__label--row">
              {8 - rowIndex}
            </div>
            {row.map((token, colIndex) => {
              const pieceToken = token as keyof typeof TOKEN_TO_PIECE;
              const piece = (TOKEN_TO_PIECE[pieceToken] ??
                null) as
                | "EMPTY"
                | "WHITE_MAN"
                | "BLACK_MAN"
                | "WHITE_KING"
                | "BLACK_KING"
                | null;
              const isDark = (rowIndex + colIndex) % 2 === 1;
              const isSelected =
                selectedCell !== null &&
                selectedCell.row === rowIndex &&
                selectedCell.col === colIndex;
              const isLegal = isLegalMove(rowIndex, colIndex);
              const isCapture = isCaptureCell(rowIndex, colIndex);
              const cellOwner = piece
                ? piece.startsWith("WHITE")
                  ? "WHITE"
                  : "BLACK"
                : null;

              // Можно кликать только по своей шашке или по допустимому ходу
              const canClick =
                cellOwner === humanColor ||
                (isLegal && currentPlayerColor === humanColor);

              return (
                <Cell
                  key={`${rowIndex}-${colIndex}`}
                  row={rowIndex}
                  col={colIndex}
                  piece={piece}
                  isDark={isDark}
                  isSelected={isSelected}
                  isLegalMove={isLegal}
                  isCapture={isCapture}
                  onClick={canClick ? handleCellClick : (() => {})}
                />
              );
            })}
            {/* Номер строки (справа) */}
            <div className="board__label board__label--row">
              {8 - rowIndex}
            </div>
          </div>
        ))}
      </div>

      {/* Номера колонок (низ) */}
      <div className="board__labels board__labels--bottom">
        {COLUMN_LABELS.map((c) => (
          <div key={c} className="board__label">
            {c}
          </div>
        ))}
      </div>
    </div>
  );
}
