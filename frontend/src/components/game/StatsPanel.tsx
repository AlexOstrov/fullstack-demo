/**
 * Блок статистики побед «человек / ИИ» (B1).
 * Источник — GET /api/tournament (агрегация по таблице tournament).
 */

import type { TournamentStatsResponse } from "../../api/types";

interface StatsPanelProps {
  stats: TournamentStatsResponse | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

export default function StatsPanel({
  stats,
  isLoading,
  error,
  onRefresh,
}: StatsPanelProps) {
  const total = stats?.total ?? 0;

  return (
    <section className="panel stats" aria-labelledby="stats-title">
      <div className="panel__header">
        <h2 className="panel__title" id="stats-title">
          Счёт побед
        </h2>
        <button
          type="button"
          className="stats__refresh"
          onClick={onRefresh}
          disabled={isLoading}
        >
          {isLoading ? "Обновляем…" : "Обновить"}
        </button>
      </div>

      {error && (
        <p className="stats__error" role="status">
          {error}
        </p>
      )}

      {isLoading && !stats ? (
        <p className="stats__empty">Загружаем статистику…</p>
      ) : total === 0 || !stats ? (
        <p className="stats__empty">
          Пока нет завершённых партий — сыграйте первую.
        </p>
      ) : (
        <>
          <div
            className="stats__bar"
            role="img"
            aria-label={`Всего партий: ${total}. Победы человека: ${stats.humanWins}, победы ИИ: ${stats.aiWins}, ничьи: ${stats.draws}.`}
          >
            <span
              className="stats__bar-part stats__bar-part--human"
              style={{ flexGrow: stats.humanWins }}
            />
            <span
              className="stats__bar-part stats__bar-part--ai"
              style={{ flexGrow: stats.aiWins }}
            />
            <span
              className="stats__bar-part stats__bar-part--draw"
              style={{ flexGrow: stats.draws }}
            />
          </div>

          <dl className="stats__list">
            <div className="stats__row">
              <dt>
                <span
                  className="stats__dot stats__dot--human"
                  aria-hidden="true"
                />
                Вы
              </dt>
              <dd>{stats.humanWins}</dd>
            </div>
            <div className="stats__row">
              <dt>
                <span className="stats__dot stats__dot--ai" aria-hidden="true" />
                ИИ
              </dt>
              <dd>{stats.aiWins}</dd>
            </div>
            <div className="stats__row">
              <dt>
                <span
                  className="stats__dot stats__dot--draw"
                  aria-hidden="true"
                />
                Ничьи
              </dt>
              <dd>{stats.draws}</dd>
            </div>
          </dl>

          <p className="stats__total">Всего партий: {total}</p>
        </>
      )}
    </section>
  );
}
