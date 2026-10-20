/**
 * Комментарий ИИ к ходу: текст, эмоция, источник (LLM/fallback), лайк.
 */

import { useCallback, useState } from "react";
import { likeComment } from "../../api/client";
import type { AiTurnDto } from "../../api/types";

interface CommentaryBoxProps {
  ai: AiTurnDto | null;
  gameId: string;
}

const EMOJI_BY_EMOTION: Record<string, string> = {
  NEUTRAL: "😐",
  EXCITED: "😄",
  CONFIDENT: "😎",
  CAUTIOUS: "🤔",
};

export default function CommentaryBox({
  ai,
  gameId,
}: CommentaryBoxProps) {
  const [liked, setLiked] = useState(ai?.liked ?? false);
  const [isLiking, setIsLiking] = useState(false);

  const handleLike = useCallback(async (nextLiked: boolean) => {
    const commentId = ai?.id;
    if (commentId === undefined || commentId === null) return;
    setIsLiking(true);
    try {
      await likeComment(gameId, commentId, nextLiked);
      setLiked(nextLiked);
    } catch {
      // ignore
    } finally {
      setIsLiking(false);
    }
  }, [ai?.id, gameId]);

  if (!ai) return null;

  return (
    <div className="commentary-box">
      <div className="commentary-header">
        <span className="commentary-emoji">
          {EMOJI_BY_EMOTION[ai.emotion] || "😐"}
        </span>
        <span className="commentary-source">
          {ai.commentSource === "LLM" ? "ИИ" : "Шаблон"}
        </span>
        <button
          className={`like-button ${liked ? "liked" : ""} ${isLiking ? "liking" : ""}`}
          onClick={() => handleLike(!liked)}
          aria-label={liked ? "Убрать лайк" : "Поставить лайк"}
        >
          👍
        </button>
      </div>
      <div className="commentary-text">{ai.comment}</div>
    </div>
  );
}
