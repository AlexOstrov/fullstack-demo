/**
 * Карточка игрока: цвет, описание, количество шашек и индикатор хода.
 */

import type { PlayerColor } from "../../api/types";

interface PlayerCardProps {
  label: string;
  meta: string;
  color: PlayerColor;
  pieceCount: number;
  isTurn: boolean;
  isThinking?: boolean;
}

/** Склонение слова «шашка» по числу. */
function piecesWord(count: number): string {
  const last = count % 10;
  const lastTwo = count % 100;
  if (last === 1 && lastTwo !== 11) return "шашка";
  if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "шашки";
  return "шашек";
}

export default function PlayerCard({
  label,
  meta,
  color,
  pieceCount,
  isTurn,
  isThinking = false,
}: PlayerCardProps) {
  return (
    <div className={`player-card${isTurn ? " player-card--active" : ""}`}>
      <span
        className={`player-card__piece player-card__piece--${color.toLowerCase()}`}
        aria-hidden="true"
      />
      <div className="player-card__info">
        <span className="player-card__name">{label}</span>
        <span className="player-card__meta">{meta}</span>
      </div>
      {isTurn && (
        <span className="player-card__turn">
          <span className="chip__dot" aria-hidden="true" />
          {isThinking ? "думает" : "ходит"}
        </span>
      )}
      <span className="player-card__count">
        <b>{pieceCount}</b>
        <span>{piecesWord(pieceCount)}</span>
      </span>
    </div>
  );
}
