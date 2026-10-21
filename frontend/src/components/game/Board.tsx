/**
 * Игровая доска 8×8. Рендерится из состояния бэкенда (GameStateResponse).
 * Координаты вынесены за пределы поля, доска переворачивается под цвет игрока,
 * поддерживается выбор шашки, подсветка ходов, последний ход и анимация.
 */

import { useMemo, type ReactElement } from "react";
import type { LegalMoveDto, MoveDto, PlayerColor } from "../../api/types";
import Cell, { type PieceType } from "./Cell";

interface BoardProps {
  board: string[][]; // компактные токены: "", "w", "W", "b", "B"
  legalMoves: LegalMoveDto[];
  currentPlayerColor: PlayerColor;
  humanColor: PlayerColor;
  selectedCell: { row: number; col: number } | null;
  lastMove: MoveDto | null;
  onCellClick: (row: number, col: number) => void;
  animatingFrom: { row: number; col: number } | null;
  animatingTo: { row: number; col: number } | null;
  animationType: "move" | "capture" | "promote" | null;
  /** Можно ли сейчас взаимодействовать с доской (ход игрока). */
  interactive?: boolean;
}

const TOKEN_TO_PIECE: Record<string, PieceType | null> = {
  "": null,
  w: "WHITE_MAN",
  W: "WHITE_KING",
  b: "BLACK_MAN",
  B: "BLACK_KING",
};

const INDICES = [0, 1, 2, 3, 4, 5, 6, 7];

/** Является ли ход взятием: хотя бы один отрезок пути длиннее одной клетки. */
function isCapturePath(path: LegalMoveDto["path"]): boolean {
  for (let i = 1; i < path.length; i += 1) {
    const prev = path[i - 1];
    const next = path[i];
    if (Math.abs(next.row - prev.row) > 1 || Math.abs(next.col - prev.col) > 1) {
      return true;
    }
  }
  return false;
}

function sameCell(
  a: { row: number; col: number } | null | undefined,
  b: { row: number; col: number },
): boolean {
  return Boolean(a && a.row === b.row && a.col === b.col);
}

export default function Board({
  board,
  legalMoves,
  currentPlayerColor,
  humanColor,
  selectedCell,
  lastMove,
  onCellClick,
  animatingFrom,
  animatingTo,
  animationType,
  interactive = true,
}: BoardProps) {
  const flipped = humanColor === "BLACK";

  /** Экранные координаты (0…7) → координаты состояния партии. */
  const toBoard = (displayRow: number, displayCol: number) => ({
    row: flipped ? 7 - displayRow : displayRow,
    col: flipped ? 7 - displayCol : displayCol,
  });

  const toDisplay = (row: number, col: number) => ({
    row: flipped ? 7 - row : row,
    col: flipped ? 7 - col : col,
  });

  const targets = useMemo(() => {
    const map = new Map<string, { capture: boolean }>();
    // Ходы соперника не подсвечиваем и показываем цели только для выбранной
    // шашки — так доска остаётся читаемой.
    if (!interactive || currentPlayerColor !== humanColor || !selectedCell) {
      return map;
    }
    for (const move of legalMoves) {
      if (move.path.length < 2) continue;
      const start = move.path[0];
      if (start.row !== selectedCell.row || start.col !== selectedCell.col) {
        continue;
      }
      const last = move.path[move.path.length - 1];
      map.set(`${last.row}-${last.col}`, {
        capture: isCapturePath(move.path),
      });
    }
    return map;
  }, [legalMoves, interactive, currentPlayerColor, humanColor, selectedCell]);

  const lastMoveCells = useMemo(() => {
    const set = new Set<string>();
    lastMove?.path.forEach((cell) => set.add(`${cell.row}-${cell.col}`));
    return set;
  }, [lastMove]);

  const animationSource = animatingFrom ? toDisplay(animatingFrom.row, animatingFrom.col) : null;
  const animationTarget = animatingTo ? toDisplay(animatingTo.row, animatingTo.col) : null;
  const animDx = animationSource && animationTarget ? animationTarget.col - animationSource.col : 0;
  const animDy = animationSource && animationTarget ? animationTarget.row - animationSource.row : 0;

  const cells: ReactElement[] = [];

  for (let displayRow = 0; displayRow < 8; displayRow += 1) {
    for (let displayCol = 0; displayCol < 8; displayCol += 1) {
      const { row, col } = toBoard(displayRow, displayCol);
      const token = board[row]?.[col] ?? "";
      const piece = TOKEN_TO_PIECE[token] ?? null;
      const isDark = (row + col) % 2 === 1;
      const owner: PlayerColor | null = piece
        ? piece.startsWith("WHITE")
          ? "WHITE"
          : "BLACK"
        : null;
      const target = targets.get(`${row}-${col}`);
      const isLegal = Boolean(target);
      const isOwnPiece = owner === humanColor;
      const canClick =
        interactive && (isOwnPiece || (isLegal && currentPlayerColor === humanColor));

      const isAnimatingFrom = Boolean(
        animatingFrom && sameCell(animatingFrom, { row, col }),
      );
      const isAnimatingTo = Boolean(
        animatingTo && sameCell(animatingTo, { row, col }),
      );

      cells.push(
        <Cell
          key={`${row}-${col}`}
          row={row}
          col={col}
          piece={piece}
          isDark={isDark}
          isSelected={sameCell(selectedCell, { row, col })}
          isLegalMove={isLegal}
          isCapture={Boolean(target?.capture)}
          isLastMove={lastMoveCells.has(`${row}-${col}`)}
          isAnimatingFrom={isAnimatingFrom}
          isAnimatingTo={isAnimatingTo}
          animationType={animationType}
          animDx={animDx}
          animDy={animDy}
          interactive={canClick}
          onClick={onCellClick}
        />,
      );
    }
  }

  return (
    <div className="board-frame">
      <div
        className="board"
        role="grid"
        aria-label={`Игровая доска 8×8, вы играете ${
          humanColor === "WHITE" ? "белыми" : "чёрными"
        }`}
        aria-rowcount={8}
        aria-colcount={8}
      >
        <div className="board__ranks" aria-hidden="true">
          {INDICES.map((index) => (
            <span key={index}>{flipped ? index + 1 : 8 - index}</span>
          ))}
        </div>

        <div className="board__squares">{cells}</div>

        <div className="board__files" aria-hidden="true">
          {INDICES.map((index) => (
            <span key={index}>
              {String.fromCharCode(flipped ? 104 - index : 97 + index)}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
