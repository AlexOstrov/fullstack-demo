/**
 * Утилиты для компактных токенов доски бэкенда: "", "w", "W", "b", "B".
 */

import type { PlayerColor } from "../../api/types";

/** Цвет шашки по токену. */
export function tokenColor(token: string): PlayerColor | null {
  if (token === "w" || token === "W") return "WHITE";
  if (token === "b" || token === "B") return "BLACK";
  return null;
}

/** Принадлежит ли шашка в клетке игроку указанного цвета. */
export function isOwnPieceToken(token: string, color: PlayerColor): boolean {
  return tokenColor(token) === color;
}
