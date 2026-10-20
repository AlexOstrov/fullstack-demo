/**
 * Информационная панель: чей ход, счёт шашек, статус, ошибки.
 */

interface StatusBarProps {
  humanColor: string;
  currentPlayerColor: string;
  status: string;
  endReason: string | null;
  moveNumber: number;
  statusMessage: string | null;
  errorMessage: string | null;
}

export default function StatusBar({
  humanColor,
  currentPlayerColor,
  status,
  moveNumber,
  statusMessage,
  errorMessage,
}: StatusBarProps) {
  const isFinished = status !== "IN_PROGRESS";
  const isHumanTurn = currentPlayerColor === humanColor;

  return (
    <div className="status-bar">
      <span>
        Вы{" "}
        <strong>{humanColor === "WHITE" ? "белые" : "чёрные"}</strong>
      </span>
      <span>
        Шашки{" "}
        <strong>— : —</strong>
      </span>
      <span>
        Ход{" "}
        <strong>{isHumanTurn ? "ваш" : "ИИ"}</strong>
      </span>
      <span>Ход №{moveNumber}</span>
      {errorMessage && (
        <span className="status-bar__error">{errorMessage}</span>
      )}
      {isFinished && statusMessage && (
        <span className="status-bar__status">{statusMessage}</span>
      )}
    </div>
  );
}
