/**
 * Панель настроек партии: цвет, сложность, стиль комментария и модель LLM.
 * Настройки можно менять в любой момент — они применяются по кнопке
 * «Начать партию» (текущая партия заменяется новой).
 */

import type {
  CommentaryStyle,
  Difficulty,
  PlayerColor,
  StartGameRequest,
} from "../../api/types";

interface SettingsPanelProps {
  settings: StartGameRequest;
  models: string[];
  isLoadingModels: boolean;
  isStarting: boolean;
  hasActiveGame: boolean;
  onSettingsChange: (settings: StartGameRequest) => void;
  onStartGame: (prefs: StartGameRequest) => void;
}

const COLOR_LABEL: Record<PlayerColor, string> = {
  WHITE: "Белые",
  BLACK: "Чёрные",
};

const DIFFICULTY_OPTIONS: { value: Difficulty; label: string }[] = [
  { value: "EASY", label: "Легко" },
  { value: "MEDIUM", label: "Средне" },
  { value: "HARD", label: "Сложно" },
  { value: "CUSTOM", label: "Свои" },
];

const STYLE_LABEL: Record<CommentaryStyle, string> = {
  FRIENDLY_MASTER: "Дружелюбный мастер",
  COMPETITIVE: "Соревновательный",
  NEUTRAL: "Нейтральный",
};

const STYLE_OPTIONS = Object.keys(STYLE_LABEL) as CommentaryStyle[];

export default function SettingsPanel({
  settings,
  models,
  isLoadingModels,
  isStarting,
  hasActiveGame,
  onSettingsChange,
  onStartGame,
}: SettingsPanelProps) {
  const update = (patch: Partial<StartGameRequest>) => {
    onSettingsChange({ ...settings, ...patch });
  };

  const handleColorChange = (playerColor: PlayerColor) => {
    update({ playerColor });
  };

  const handleDifficultyChange = (difficulty: Difficulty) => {
    update({
      difficulty,
      aiDepth: difficulty === "CUSTOM" ? (settings.aiDepth ?? 6) : null,
      aiTimeBudgetMs:
        difficulty === "CUSTOM" ? (settings.aiTimeBudgetMs ?? 1500) : null,
    });
  };

  const handleNumberChange = (
    field: "aiDepth" | "aiTimeBudgetMs",
    value: string,
    fallback: number,
  ) => {
    const parsed = Number.parseInt(value, 10);
    update({ [field]: Number.isFinite(parsed) ? parsed : fallback });
  };

  const modelOptions = models.length > 0 ? models : [settings.llmModel];

  return (
    <section className="panel settings" aria-labelledby="settings-title">
      <div className="panel__header">
        <h2 className="panel__title" id="settings-title">
          Настройки партии
        </h2>
      </div>

      <div className="settings__group">
        <span className="settings__label" id="settings-color-label">
          Ваш цвет
        </span>
        <div
          className="segmented segmented--two"
          role="group"
          aria-labelledby="settings-color-label"
        >
          {(["WHITE", "BLACK"] as PlayerColor[]).map((color) => (
            <button
              key={color}
              type="button"
              className="segmented__option"
              aria-pressed={settings.playerColor === color}
              onClick={() => handleColorChange(color)}
            >
              <span
                className={`segmented__piece segmented__piece--${color.toLowerCase()}`}
                aria-hidden="true"
              />
              {COLOR_LABEL[color]}
            </button>
          ))}
        </div>
        <span className="settings__hint">
          {settings.playerColor === "WHITE"
            ? "Белые ходят первыми"
            : "Первым ходит ИИ"}
        </span>
      </div>

      <div className="settings__group">
        <span className="settings__label" id="settings-difficulty-label">
          Сложность ИИ
        </span>
        <div
          className="segmented segmented--four"
          role="group"
          aria-labelledby="settings-difficulty-label"
        >
          {DIFFICULTY_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              className="segmented__option"
              aria-pressed={settings.difficulty === option.value}
              onClick={() => handleDifficultyChange(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>

        {settings.difficulty === "CUSTOM" && (
          <div className="settings__custom">
            <label className="field">
              <span className="field__label">Глубина</span>
              <input
                className="input"
                type="number"
                min={1}
                max={12}
                value={settings.aiDepth ?? 6}
                onChange={(event) =>
                  handleNumberChange("aiDepth", event.target.value, 6)
                }
              />
            </label>
            <label className="field">
              <span className="field__label">Бюджет, мс</span>
              <input
                className="input"
                type="number"
                min={200}
                max={5000}
                step={100}
                value={settings.aiTimeBudgetMs ?? 1500}
                onChange={(event) =>
                  handleNumberChange(
                    "aiTimeBudgetMs",
                    event.target.value,
                    1500,
                  )
                }
              />
            </label>
          </div>
        )}
      </div>

      <label className="field">
        <span className="field__label">Стиль комментария</span>
        <select
          className="select"
          value={settings.commentaryStyle}
          onChange={(event) =>
            update({
              commentaryStyle: event.target.value as CommentaryStyle,
            })
          }
        >
          {STYLE_OPTIONS.map((style) => (
            <option key={style} value={style}>
              {STYLE_LABEL[style]}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span className="field__label">
          Модель LLM
          {isLoadingModels && (
            <span className="settings__hint"> — загружаем…</span>
          )}
        </span>
        <select
          className="select"
          value={settings.llmModel}
          disabled={isLoadingModels}
          onChange={(event) => update({ llmModel: event.target.value })}
        >
          {modelOptions.map((model) => (
            <option key={model} value={model}>
              {model}
            </option>
          ))}
        </select>
      </label>

      <button
        type="button"
        className="button button--primary button--block settings__start"
        disabled={isStarting}
        onClick={() => onStartGame(settings)}
      >
        {isStarting
          ? "Создаём партию…"
          : hasActiveGame
            ? "Начать новую партию"
            : "Начать партию"}
      </button>

      <p className="settings__note">
        {hasActiveGame
          ? "Текущая партия будет заменена новой."
          : "Выберите параметры и начните игру."}
      </p>
    </section>
  );
}
