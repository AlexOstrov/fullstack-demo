/**
 * Одна клетка доски с шашкой (или без).
 * Подсветка: выбранная клетка, допустимый ход, взятие, последний ход,
 * анимация перемещения и появление короны при превращении.
 */

import type { CSSProperties } from "react";
import CrownIcon from "./CrownIcon";

export type PieceType =
  | "WHITE_MAN"
  | "BLACK_MAN"
  | "WHITE_KING"
  | "BLACK_KING";

interface CellProps {
  row: number;
  col: number;
  piece: PieceType | null;
  isDark: boolean;
  isSelected: boolean;
  isLegalMove: boolean;
  isCapture: boolean;
  isLastMove: boolean;
  isAnimatingFrom: boolean;
  isAnimatingTo: boolean;
  animationType: "move" | "capture" | "promote" | null;
  /** Смещение анимации в клетках (в экранных координатах, с учётом переворота). */
  animDx: number;
  animDy: number;
  interactive: boolean;
  onClick: (row: number, col: number) => void;
}

const PIECE_LABEL: Record<PieceType, string> = {
  WHITE_MAN: "белая шашка",
  BLACK_MAN: "чёрная шашка",
  WHITE_KING: "белая дамка",
  BLACK_KING: "чёрная дамка",
};

/** Шахматная нотация клетки: столбцы a–h, горизонтали 1–8. */
function squareName(row: number, col: number): string {
  return `${String.fromCharCode(97 + col)}${8 - row}`;
}

export default function Cell({
  row,
  col,
  piece,
  isDark,
  isSelected,
  isLegalMove,
  isCapture,
  isLastMove,
  isAnimatingFrom,
  isAnimatingTo,
  animationType,
  animDx,
  animDy,
  interactive,
  onClick,
}: CellProps) {
  const label = squareName(row, col);
  const hasPiece = piece !== null;
  const color = piece?.startsWith("WHITE") ? "white" : "black";
  const isKing = Boolean(piece?.endsWith("_KING"));

  const pieceClass = [
    "piece",
    `piece--${color}`,
    isKing ? "piece--king" : "",
    isAnimatingFrom && animationType === "capture" ? "piece--jump" : "",
    isAnimatingFrom && animationType === "promote" ? "piece--promoting" : "",
  ]
    .filter(Boolean)
    .join(" ");

  const animationStyle = isAnimatingFrom
    ? ({
        "--dx": `${animDx * 100}%`,
        "--dy": `${animDy * 100}%`,
      } as CSSProperties)
    : undefined;

  const className = [
    "board__cell",
    isDark ? "board__cell--dark" : "board__cell--light",
    isSelected ? "board__cell--selected" : "",
    isLegalMove ? "board__cell--legal" : "",
    isCapture ? "board__cell--capture" : "",
    isLastMove ? "board__cell--last-move" : "",
    isAnimatingFrom ? "board__cell--animating-from" : "",
    isAnimatingTo ? "board__cell--animating-to" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type="button"
      role="gridcell"
      aria-label={`клетка ${label}${hasPiece ? `, ${PIECE_LABEL[piece]}` : ""}`}
      className={className}
      style={animationStyle}
      disabled={!interactive}
      onClick={() => onClick(row, col)}
    >
      {hasPiece && (
        <span className={pieceClass} aria-hidden="true">
          <span className="piece__body">
            {isKing && <CrownIcon className="piece__crown" />}
          </span>
        </span>
      )}
    </button>
  );
}
