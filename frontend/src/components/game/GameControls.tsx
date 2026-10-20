/**
 * Управление игрой: сдача, ничья, рестарт.
 */

import { useCallback } from "react";
import { resign, offerDraw, restartGame } from "../../api/client";
import type { StartGameRequest } from "../../api/types";

interface GameControlsProps {
  gameId: string;
  status: string;
  endReason: string | null;
  currentPlayerColor: string;
  humanColor: string;
  preferences: StartGameRequest;
  onGameOver: () => void;
  onRestart: (request: StartGameRequest) => void;
}

export default function GameControls({
  gameId,
  status,
  endReason,
  currentPlayerColor,
  humanColor,
  preferences,
  onGameOver,
  onRestart,
}: GameControlsProps) {
  const isFinished = status !== "IN_PROGRESS";
  const isHumanTurn = currentPlayerColor === humanColor;

  const handleResign = useCallback(async () => {
    try {
      await resign(gameId);
      onGameOver();
    } catch {
      // ignore
    }
  }, [gameId, onGameOver]);

  const handleDraw = useCallback(async () => {
    try {
      const result = await offerDraw(gameId);
      if (result.accepted) {
        onGameOver();
      }
    } catch {
      // ignore
    }
  }, [gameId, onGameOver]);

  const handleRestart = useCallback(async () => {
    try {
      await restartGame(gameId, preferences);
      onRestart(preferences);
    } catch {
      // ignore
    }
  }, [gameId, preferences, onRestart]);

  return (
    <div className="game-controls">
      <button
        className="button button--resign"
        disabled={isFinished || !isHumanTurn}
        onClick={handleResign}
      >
        Сдаться
      </button>
      <button
        className="button button--draw"
        disabled={isFinished || !isHumanTurn}
        onClick={handleDraw}
      >
        Ничья
      </button>
      <button
        className="button button--restart"
        onClick={handleRestart}
      >
        Заново
      </button>
      {isFinished && (
        <span className="game-status">
          {endReason === "RESIGN"
            ? currentPlayerColor === humanColor
              ? "Вы сдались — победа ИИ"
              : "ИИ сдался — ваша победа"
            : endReason === "DRAW_AGREEMENT"
            ? "Ничья по согласию"
            : endReason === "DRAW_REPETITION"
            ? "Ничья — троекратное повторение"
            : endReason === "DRAW_KINGS"
            ? "Ничья — только дамки"
            : "Игра окончена"}
        </span>
      )}
    </div>
  );
}
