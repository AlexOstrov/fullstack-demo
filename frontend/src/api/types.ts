/**
 * Типы API бэкенда (соответствуют Java DTO в backend/game-service/.../dto/game/).
 * См. ТЗ раздел 5 (REST API) и 5.1 (DTO records).
 */

/** Цвет игрока/шашки. */
export type PlayerColor = "WHITE" | "BLACK";

/** Сложность ИИ. */
export type Difficulty = "EASY" | "MEDIUM" | "HARD" | "CUSTOM";

/** Стиль комментария LLM. */
export type CommentaryStyle = "FRIENDLY_MASTER" | "COMPETITIVE" | "NEUTRAL";

/** Источник комментария. */
export type CommentSource = "LLM" | "FALLBACK";

/** Эмоциональная окраска комментария. */
export type Emotion = "NEUTRAL" | "EXCITED" | "CONFIDENT" | "CAUTIOUS";

/** Статус партии. */
export type GameStatus = "IN_PROGRESS" | "WHITE_WON" | "BLACK_WON" | "DRAW";

/** Причина завершения партии. */
export type GameEndReason =
  | "NO_MOVES"
  | "RESIGN"
  | "DRAW_REPETITION"
  | "DRAW_KINGS"
  | "DRAW_AGREEMENT";

/** Тип хода. */
export type MoveType = "NORMAL" | "CAPTURE" | "PROMOTION";

/** Клетка доски 0..7. */
export interface CellDto {
  row: number;
  col: number;
}

/** Ход: путь + тип. */
export interface MoveDto {
  path: CellDto[];
  type: MoveType;
}

/** Допустимый ход для UI (только путь). */
export interface LegalMoveDto {
  path: CellDto[];
}

/** Ход ИИ с комментарием. */
export interface AiTurnDto {
  move: MoveDto;
  comment: string;
  emotion: Emotion;
  commentSource: CommentSource;
  liked: boolean;
  id: number | null;
}

/** Состояние партии в ответах API. */
export interface GameStateResponse {
  gameId: string;
  humanColor: PlayerColor;
  aiColor: PlayerColor;
  status: GameStatus;
  currentPlayer: PlayerColor;
  moveNumber: number;
  version: number;
  board: string[][]; // компактные токены: "", "w", "W", "b", "B"
  lastMove: MoveDto | null;
  legalMoves: LegalMoveDto[];
  forcedCapture: boolean;
  endReason: GameEndReason | null;
  ai: AiTurnDto | null;
}

/** Запрос на создание партии. */
export interface StartGameRequest {
  playerColor: PlayerColor;
  difficulty: Difficulty;
  aiDepth?: number | null;
  aiTimeBudgetMs?: number | null;
  commentaryStyle: CommentaryStyle;
  llmModel: string;
}

/** Ход игрока. */
export interface MoveRequest {
  path: CellDto[];
  clientMoveNumber: number;
}

/** Результат сдачи/ничьи. */
export interface DrawResponse {
  accepted: boolean;
  game: GameStateResponse;
}

/** Список моделей LLM. */
export interface LlmModelsResponse {
  models: string[];
  defaultModel: string;
}

/** Комментарий в истории. */
export interface CommentDto {
  id: number | null;
  moveNumber: number;
  text: string;
  model: string | null;
  commentSource: CommentSource;
  liked: boolean;
}

/** Запись истории ходов. */
export interface MoveRecordDto {
  moveNumber: number;
  player: PlayerColor;
  type: MoveType;
  path: CellDto[];
}

/** История ходов и комментариев. */
export interface MovesResponse {
  moves: MoveRecordDto[];
  comments: CommentDto[];
}

/** Запрос на лайк комментария. */
export interface LikedRequest {
  liked: boolean;
}

/** Ошибка API в едином формате (ТЗ 5.3). */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
}
