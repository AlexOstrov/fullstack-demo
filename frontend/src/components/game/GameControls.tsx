/**
 * Управление партией: сдача и предложение ничьей.
 * «Новая партия» находится в панели настроек.
 */

import { useCallback, useState } from "react";
import { offerDraw, resign } from "../../api/client";
import type { GameStateResponse, GameStatus } from "../../api/types";

interface GameControlsProps {
  gameId: string;
  status: GameStatus;
  canAct: boolean;
  onStateUpdate: (state: GameStateResponse) => void;
  onError: (message: string) => void;
}

export default function GameControls({
  gameId,
  status,
  canAct,
  onStateUpdate,
  onError,
}: GameControlsProps) {
  const [busy, setBusy] = useState(false);
  const isFinished = status !== "IN_PROGRESS";

  const handleResign = useCallback(async () => {
    if (!gameId) return;
    setBusy(true);
    try {
      const state = await resign(gameId);
      onStateUpdate(state);
    } catch (err) {
      onError(err instanceof Error ? err.message : "Не удалось сдать партию");
    } finally {
      setBusy(false);
    }
  }, [gameId, onStateUpdate, onError]);

  const handleDraw = useCallback(async () => {
    if (!gameId) return;
    setBusy(true);
    try {
      const result = await offerDraw(gameId);
      onStateUpdate(result.game);
    } catch (err) {
      onError(
        err instanceof Error ? err.message : "Не удалось предложить ничью",
      );
    } finally {
      setBusy(false);
    }
  }, [gameId, onStateUpdate, onError]);

  const disabled = isFinished || !canAct || busy;

  return (
    <div className="game-controls">
      <button
        type="button"
        className="button button--danger"
        disabled={disabled}
        onClick={() => void handleResign()}
      >
        Сдаться
      </button>
      <button
        type="button"
        className="button"
        disabled={disabled}
        onClick={() => void handleDraw()}
      >
        Ничья
      </button>
    </div>
  );
}
