/**
 * История ходов в шахматной нотации (a1–h8).
 */

import { useEffect, useRef } from "react";
import type { MoveRecordDto } from "../../api/types";

interface MoveHistoryProps {
  moves: MoveRecordDto[];
}

function cellName(row: number, col: number): string {
  return `${String.fromCharCode(97 + col)}${8 - row}`;
}

function notation(move: MoveRecordDto): string {
  if (move.path.length === 0) return "—";
  const separator = move.type === "CAPTURE" ? "×" : "–";
  return move.path
    .map((cell) => cellName(cell.row, cell.col))
    .join(separator);
}

function movesWord(count: number): string {
  const last = count % 10;
  const lastTwo = count % 100;
  if (last === 1 && lastTwo !== 11) return "ход";
  if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "хода";
  return "ходов";
}

export default function MoveHistory({ moves }: MoveHistoryProps) {
  const listRef = useRef<HTMLOListElement>(null);

  useEffect(() => {
    const list = listRef.current;
    if (list) {
      list.scrollTop = list.scrollHeight;
    }
  }, [moves.length]);

  return (
    <section className="panel" aria-labelledby="moves-title">
      <div className="panel__header">
        <h2 className="panel__title" id="moves-title">
          История ходов
        </h2>
        <span className="panel__hint">
          {moves.length > 0
            ? `${moves.length} ${movesWord(moves.length)}`
            : "пока пусто"}
        </span>
      </div>

      {moves.length === 0 ? (
        <p className="moves__empty">
          Ходы появятся здесь после начала партии.
        </p>
      ) : (
        <ol className="moves" ref={listRef}>
          {moves.map((move, index) => (
            <li className="moves__row" key={`${move.moveNumber}-${index}`}>
              <span className="moves__num">{move.moveNumber}.</span>
              <span
                className={`moves__side moves__side--${
                  move.player === "WHITE" ? "wh" : "bl"
                }`}
                aria-label={move.player === "WHITE" ? "белые" : "чёрные"}
              />
              <span className="moves__notation">
                {notation(move)}
                {move.type === "PROMOTION" && (
                  <span className="moves__tag">дамка</span>
                )}
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
