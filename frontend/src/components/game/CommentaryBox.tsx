/**
 * Комментарий ИИ к ходу: текст, эмоция, источник (LLM/шаблон),
 * оценка комментария «нравится / не нравится».
 */

import { useCallback, useState } from "react";
import { likeComment } from "../../api/client";
import type { AiTurnDto, Emotion } from "../../api/types";

interface CommentaryBoxProps {
  ai: AiTurnDto | null;
  gameId: string;
  isThinking: boolean;
}

const EMOJI_BY_EMOTION: Record<Emotion, string> = {
  NEUTRAL: "😐",
  EXCITED: "😄",
  CONFIDENT: "😎",
  CAUTIOUS: "🤔",
};

type Feedback = "up" | "down" | null;

export default function CommentaryBox({
  ai,
  gameId,
  isThinking,
}: CommentaryBoxProps) {
  const [override, setOverride] = useState<{
    id: number | null;
    value: Feedback;
  } | null>(null);
  const [isLiking, setIsLiking] = useState(false);

  const commentId = ai?.id ?? null;
  // Показываем оценку только после действия игрока: бэкенд сохраняет
  // комментарии автоматически, и `liked=true` не означает, что игрок их оценил.
  const feedback: Feedback =
    override && override.id === commentId ? override.value : null;

  const sendFeedback = useCallback(
    async (next: Feedback) => {
      if (commentId === null) return;
      if (next === feedback) return;
      setIsLiking(true);
      try {
        await likeComment(gameId, commentId, next === "up");
        setOverride({ id: commentId, value: next });
      } catch {
        // Оценка комментария не критична — молча игнорируем сбой.
      } finally {
        setIsLiking(false);
      }
    },
    [commentId, feedback, gameId],
  );

  const hasComment = Boolean(ai?.comment);

  return (
    <section className="panel commentary" aria-labelledby="commentary-title">
      <div className="commentary__head">
        <span className="commentary__avatar" aria-hidden="true">
          {ai ? EMOJI_BY_EMOTION[ai.emotion] ?? "😐" : "💬"}
        </span>
        <span className="commentary__title">
          <b id="commentary-title">Комментарий ИИ</b>
          <span>Живой разбор партии от соперника</span>
        </span>
        {ai && (
          <span
            className={`commentary__source${
              ai.commentSource === "LLM" ? " commentary__source--llm" : ""
            }`}
          >
            {ai.commentSource === "LLM" ? "LLM" : "Шаблон"}
          </span>
        )}
      </div>

      {hasComment ? (
        <blockquote className="commentary__text">{ai?.comment}</blockquote>
      ) : isThinking ? (
        <p className="commentary__empty">
          ИИ обдумывает ход
          <span className="commentary__thinking" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
        </p>
      ) : (
        <p className="commentary__empty">
          Сделайте ход — соперник прокомментирует свою игру.
        </p>
      )}

      {hasComment && (
        <div className="commentary__actions">
          <span className="commentary__actions-label">Комментарий полезен?</span>
          <button
            type="button"
            className={`icon-button${feedback === "up" ? " icon-button--active" : ""}`}
            aria-label="Комментарий нравится"
            aria-pressed={feedback === "up"}
            disabled={commentId === null || isLiking}
            onClick={() => void sendFeedback("up")}
          >
            👍
          </button>
          <button
            type="button"
            className={`icon-button${feedback === "down" ? " icon-button--active" : ""}`}
            aria-label="Комментарий не нравится"
            aria-pressed={feedback === "down"}
            disabled={commentId === null || isLiking}
            onClick={() => void sendFeedback("down")}
          >
            👎
          </button>
        </div>
      )}

      {isThinking && hasComment && (
        <p className="commentary__empty">
          ИИ обдумывает следующий ход
          <span className="commentary__thinking" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
        </p>
      )}
    </section>
  );
}
