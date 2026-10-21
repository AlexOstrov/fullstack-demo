/**
 * Статус партии: чей ход, номер хода, обязательное взятие, ошибки и итог.
 */

import type { GameEndReason, GameStatus, PlayerColor } from "../../api/types";

interface StatusBarProps {
  hasGame: boolean;
  humanColor: PlayerColor;
  currentPlayerColor: PlayerColor;
  status: GameStatus;
  endReason: GameEndReason | null;
  moveNumber: number;
  forcedCapture: boolean;
  errorMessage: string | null;
}

/** Итог партии с точки зрения человека. */
function resultTitle(status: GameStatus, humanColor: PlayerColor): string {
  if (status === "DRAW") return "Ничья";
  const winner: PlayerColor = status === "WHITE_WON" ? "WHITE" : "BLACK";
  return winner === humanColor ? "Вы победили" : "Победил ИИ";
}

/** Пояснение причины завершения партии. */
function endReasonText(
  status: GameStatus,
  endReason: GameEndReason | null,
  humanColor: PlayerColor,
): string {
  const humanWon =
    (status === "WHITE_WON" && humanColor === "WHITE") ||
    (status === "BLACK_WON" && humanColor === "BLACK");

  switch (endReason) {
    case "NO_MOVES":
      return "У соперника не осталось допустимых ходов";
    case "RESIGN":
      return humanWon ? "ИИ сдался" : "Вы сдали партию";
    case "DRAW_REPETITION":
      return "Троекратное повторение позиции";
    case "DRAW_KINGS":
      return "15 ходов только дамками без взятий";
    case "DRAW_AGREEMENT":
      return "Ничья по согласию игроков";
    default:
      return "Партия завершена";
  }
}

export default function StatusBar({
  hasGame,
  humanColor,
  currentPlayerColor,
  status,
  endReason,
  moveNumber,
  forcedCapture,
  errorMessage,
}: StatusBarProps) {
  const isFinished = status !== "IN_PROGRESS";
  const isHumanTurn = currentPlayerColor === humanColor;

  const title = !hasGame
    ? "Партия не начата"
    : isFinished
      ? resultTitle(status, humanColor)
      : isHumanTurn
        ? "Ваш ход"
        : "Ход ИИ";

  const hint = !hasGame
    ? "Выберите параметры слева и нажмите «Начать партию»"
    : isFinished
      ? endReasonText(status, endReason, humanColor)
      : isHumanTurn
        ? "Выберите шашку и клетку — доступные ходы подсвечены"
        : "Соперник обдумывает ответ…";

  const dotClass = !hasGame
    ? "status-banner__dot status-banner__dot--done"
    : isFinished
      ? "status-banner__dot status-banner__dot--done"
      : isHumanTurn
        ? "status-banner__dot"
        : "status-banner__dot status-banner__dot--ai";

  return (
    <div
      className={`status-banner${isFinished && hasGame ? " status-banner--finished" : ""}`}
      role="status"
      aria-live="polite"
    >
      <span className="status-banner__turn">
        <span className={dotClass} aria-hidden="true" />
        <strong>{title}</strong>
      </span>

      <span className="status-banner__hint">{hint}</span>

      {hasGame && !isFinished && forcedCapture && (
        <span className="chip chip--warning">Обязательное взятие</span>
      )}

      {hasGame && (
        <span className="status-banner__move">Ход №{moveNumber}</span>
      )}

      {errorMessage && (
        <span className="status-banner__error" role="alert">
          {errorMessage}
        </span>
      )}
    </div>
  );
}
