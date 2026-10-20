/**
 * Панель настроек партии: цвет, сложность, модель LLM, стиль комментария.
 */

import type { StartGameRequest } from "../../api/types";

interface SettingsPanelProps {
  settings: StartGameRequest;
  models: string[];
  isLoadingModels: boolean;
  onSettingsChange: (settings: StartGameRequest) => void;
  onStartGame: (prefs: StartGameRequest) => void;
  disabled: boolean;
}

export default function SettingsPanel({
  settings,
  models,
  isLoadingModels,
  onSettingsChange,
  onStartGame,
  disabled,
}: SettingsPanelProps) {
  const handleColorChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onSettingsChange({
      ...settings,
      playerColor: e.target.value as "WHITE" | "BLACK",
    });
  };

  const handleDifficultyChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const difficulty = e.target.value as StartGameRequest["difficulty"];
    const next: StartGameRequest = {
      ...settings,
      difficulty,
      aiDepth: difficulty === "CUSTOM" ? settings.aiDepth ?? 4 : null,
      aiTimeBudgetMs:
        difficulty === "CUSTOM" ? settings.aiTimeBudgetMs ?? 1500 : null,
    };
    onSettingsChange(next);
  };

  const handleStyleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onSettingsChange({
      ...settings,
      commentaryStyle: e.target.value as StartGameRequest["commentaryStyle"],
    });
  };

  const handleModelChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onSettingsChange({ ...settings, llmModel: e.target.value });
  };

  const handleDepthChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onSettingsChange({
      ...settings,
      aiDepth: parseInt(e.target.value, 10) || 4,
    });
  };

  const handleBudgetChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onSettingsChange({
      ...settings,
      aiTimeBudgetMs: parseInt(e.target.value, 10) || 1500,
    });
  };

  const handleStart = () => {
    onStartGame(settings);
  };

  return (
    <div className="settings-panel">
      <h3>Настройки партии</h3>
      <div className="settings-group">
        <label>
          Цвет{" "}
          <select
            value={settings.playerColor}
            onChange={handleColorChange}
            disabled={disabled}
          >
            <option value="WHITE">Белые (ходят первыми)</option>
            <option value="BLACK">Чёрные</option>
          </select>
        </label>
      </div>
      <div className="settings-group">
        <label>
          Сложность{" "}
          <select
            value={settings.difficulty}
            onChange={handleDifficultyChange}
            disabled={disabled}
          >
            <option value="EASY">Легкий</option>
            <option value="MEDIUM">Средний</option>
            <option value="HARD">Сложный</option>
            <option value="CUSTOM">Пользовательский</option>
          </select>
        </label>
        {settings.difficulty === "CUSTOM" && (
          <>
            <div className="settings-group">
              <label>
                Глубина{" "}
                <input
                  type="number"
                  min="1"
                  max="12"
                  value={settings.aiDepth ?? 4}
                  onChange={handleDepthChange}
                  disabled={disabled}
                />
              </label>
            </div>
            <div className="settings-group">
              <label>
                Бюджет (мс){" "}
                <input
                  type="number"
                  min="200"
                  max="5000"
                  value={settings.aiTimeBudgetMs ?? 1500}
                  onChange={handleBudgetChange}
                  disabled={disabled}
                />
              </label>
            </div>
          </>
        )}
      </div>
      <div className="settings-group">
        <label>
          Стиль{" "}
          <select
            value={settings.commentaryStyle}
            onChange={handleStyleChange}
            disabled={disabled}
          >
            <option value="FRIENDLY_MASTER">Дружелюбный мастер</option>
            <option value="COMPETITIVE">Соревновательный</option>
            <option value="NEUTRAL">Нейтральный</option>
          </select>
        </label>
      </div>
      <div className="settings-group">
        <label>
          Модель LLM{" "}
          <select
            value={settings.llmModel}
            onChange={handleModelChange}
            disabled={disabled || isLoadingModels}
          >
            {models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </label>
        {isLoadingModels && <span className="loading">Загрузка...</span>}
      </div>
      <button className="button button--start" onClick={handleStart} disabled={disabled}>
        Начать партию
      </button>
    </div>
  );
}
