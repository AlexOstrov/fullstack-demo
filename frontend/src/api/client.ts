/**
 * API-клиент на нативном `fetch` с типизированной обёрткой (ТЗ 8.5).
 * Все запросы идут через Vite-прокси `/api` → http://localhost:8080.
 * Ошибки от бэкенда приходят в едином формате ApiErrorResponse (ТЗ 5.3).
 */

export type {
  DrawResponse,
  GameStateResponse,
  LikedRequest,
  LlmModelsResponse,
  MoveRequest,
  MovesResponse,
  StartGameRequest,
} from "./types";

import type {
  DrawResponse,
  GameStateResponse,
  LikedRequest,
  LlmModelsResponse,
  MoveRequest,
  MovesResponse,
  StartGameRequest,
} from "./types";

/** Базовый путь API (проксируется Vite на http://localhost:8080). */
const API_BASE = "/api";

/** Исключение с кодом ошибки бэкенда. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly path: string;

  constructor(message: string, status: number, code: string, path: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.path = path;
  }
}

/** Выполнить запрос к API и вернуть распарсенное JSON-тело. */
async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let errorData: { message?: string; code?: string; path?: string } | null =
      null;
    try {
      errorData = (await response.json()) as {
        message?: string;
        code?: string;
        path?: string;
      };
    } catch {
      // ignore parse errors
    }
    throw new ApiError(
      errorData?.message || `HTTP ${response.status}`,
      response.status,
      errorData?.code || "UNKNOWN_ERROR",
      errorData?.path || path,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

/** POST /api/game/start — создать партию. */
export async function startGame(
  payload: StartGameRequest,
): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>("/game/start", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/** GET /api/game/{id} — состояние партии. */
export async function getGame(gameId: string): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>(`/game/${encodeURIComponent(gameId)}`);
}

/** GET /api/game/{id}/moves — история ходов и комментариев. */
export async function getMoves(gameId: string): Promise<MovesResponse> {
  return apiRequest<MovesResponse>(`/game/${encodeURIComponent(gameId)}/moves`);
}

/** POST /api/game/{id}/move — ход игрока. */
export async function makeMove(
  gameId: string,
  payload: MoveRequest,
): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>(`/game/${encodeURIComponent(gameId)}/move`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/** POST /api/game/{id}/ai-move — ход ИИ. */
export async function makeAiMove(
  gameId: string,
): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>(
    `/game/${encodeURIComponent(gameId)}/ai-move`,
    { method: "POST" },
  );
}

/** POST /api/game/{id}/resign — сдаться. */
export async function resign(gameId: string): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>(
    `/game/${encodeURIComponent(gameId)}/resign`,
    { method: "POST" },
  );
}

/** POST /api/game/{id}/draw — предложить ничью. */
export async function offerDraw(gameId: string): Promise<DrawResponse> {
  return apiRequest<DrawResponse>(
    `/game/${encodeURIComponent(gameId)}/draw`,
    { method: "POST" },
  );
}

/** POST /api/game/{id}/restart — начать заново. */
export async function restartGame(
  gameId: string,
  payload?: StartGameRequest,
): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>(
    `/game/${encodeURIComponent(gameId)}/restart`,
    {
      method: "POST",
      body: payload ? JSON.stringify(payload) : undefined,
      headers: payload
        ? { "Content-Type": "application/json" }
        : {},
    },
  );
}

/** GET /api/config/llm-models — список моделей LLM. */
export async function getLlmModels(): Promise<LlmModelsResponse> {
  return apiRequest<LlmModelsResponse>("/config/llm-models");
}

/** POST /api/game/{id}/comments/{commentId}/like — лайк комментария. */
export async function likeComment(
  gameId: string,
  commentId: number,
  liked: boolean,
): Promise<void> {
  return apiRequest<void>(
    `/game/${encodeURIComponent(gameId)}/comments/${commentId}/like`,
    {
      method: "POST",
      body: JSON.stringify({ liked } as LikedRequest),
    },
  );
}
