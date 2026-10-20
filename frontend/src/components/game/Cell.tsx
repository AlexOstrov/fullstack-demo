/**
 * Одна клетка доски с шашкой (или без).
 * Подсветка: выбранная клетка, допустимый ход, взятие.
 */

interface CellProps {
  row: number;
  col: number;
  piece: "EMPTY" | "WHITE_MAN" | "BLACK_MAN" | "WHITE_KING" | "BLACK_KING" | null;
  isDark: boolean;
  isSelected: boolean;
  isLegalMove: boolean;
  isCapture: boolean;
  onClick: (row: number, col: number) => void;
}

const PIECE_LABEL: Record<string, string> = {
  WHITE_MAN: "белая шашка",
  BLACK_MAN: "чёрная шашка",
  WHITE_KING: "белая дамка",
  BLACK_KING: "чёрная дамка",
};

export default function Cell({
  row,
  col,
  piece,
  isDark,
  isSelected,
  isLegalMove,
  isCapture,
  onClick,
}: CellProps) {
  const label = String.fromCharCode(97 + col) + (8 - row);
  const hasPiece = piece !== null && piece !== "EMPTY";

  return (
    <div
      role="gridcell"
      aria-label={`клетка ${label}${hasPiece ? `, ${PIECE_LABEL[piece]}` : ""}`}
      className={
        "board__cell " +
        (isDark ? "board__cell--dark" : "board__cell--light") +
        (isSelected ? " board__cell--selected" : "") +
        (isLegalMove ? " board__cell--legal" : "") +
        (isCapture ? " board__cell--capture" : "")
      }
      onClick={() => onClick(row, col)}
    >
      {piece && piece !== "EMPTY" && (
        <span
          className={`piece piece--${piece.split("_")[0].toLowerCase()}${
            piece.endsWith("_KING") ? " piece--king" : ""
          }`}
          aria-hidden="true"
        />
      )}
    </div>
  );
}
