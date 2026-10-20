/**
 * Одна клетка доски с шашкой (или без).
 * Подсветка: выбранная клетка, допустимый ход, взятие, анимация перемещения.
 */

interface CellProps {
  row: number;
  col: number;
  piece: "EMPTY" | "WHITE_MAN" | "BLACK_MAN" | "WHITE_KING" | "BLACK_KING" | null;
  isDark: boolean;
  isSelected: boolean;
  isLegalMove: boolean;
  isCapture: boolean;
  isAnimatingFrom: boolean;
  isAnimatingTo: boolean;
  animToRow: number;
  animToCol: number;
  animationType: "move" | "capture" | "promote" | null;
  onClick: (row: number, col: number) => void;
}

const PIECE_LABEL: Record<string, string> = {
  WHITE_MAN: "белая шашка",
  BLACK_MAN: "чёрная шашка",
  WHITE_KING: "белая дамка",
  BLACK_KING: "чёрная дамка",
};

// Размер клетки при максимальной ширине доски (560px / 8 = 70px)
// Используется для расчёта смещения при анимации перемещения
const CELL_SIZE = 70;

export default function Cell({
  row,
  col,
  piece,
  isDark,
  isSelected,
  isLegalMove,
  isCapture,
  isAnimatingFrom,
  isAnimatingTo,
  animToRow,
  animToCol,
  animationType,
  onClick,
}: CellProps) {
  const label = String.fromCharCode(97 + col) + (8 - row);
  const hasPiece = piece !== null && piece !== "EMPTY";

  // Вычисляем смещение для анимации перемещения шашки
  const dx = isAnimatingFrom ? (animToCol - col) * CELL_SIZE : 0;
  const dy = isAnimatingFrom ? (animToRow - row) * CELL_SIZE : 0;

  const pieceStyle = isAnimatingFrom
    ? {
        "--dx": `${dx}px`,
        "--dy": `${dy}px`,
        animation: `movePiece 300ms ease-out forwards`,
      }
    : undefined;

  return (
    <div
      role="gridcell"
      aria-label={`клетка ${label}${hasPiece ? `, ${PIECE_LABEL[piece]}` : ""}`}
      className={
        "board__cell " +
        (isDark ? "board__cell--dark" : "board__cell--light") +
        (isSelected ? " board__cell--selected" : "") +
        (isLegalMove ? " board__cell--legal" : "") +
        (isCapture ? " board__cell--capture" : "") +
        (isAnimatingFrom ? " board__cell--animating-from" : "") +
        (isAnimatingTo ? " board__cell--animating-to" : "")
      }
      onClick={(e) => {
        e.stopPropagation();
        onClick(row, col);
      }}
    >
      {piece && piece !== "EMPTY" && (
        <span
          className={`piece piece--${piece.split("_")[0].toLowerCase()}${
            piece.endsWith("_KING") ? " piece--king" : ""
          }${animationType === "promote" ? " piece--promoting" : ""}${
            animationType === "capture" ? " piece--capture-jump" : ""
          }`}
          style={pieceStyle}
          aria-hidden="true"
        />
      )}
    </div>
  );
}
