import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import SettingsPanel from "./SettingsPanel";
import type { StartGameRequest } from "../../api/types";

const settings: StartGameRequest = {
  playerColor: "WHITE",
  difficulty: "EASY",
  aiDepth: null,
  aiTimeBudgetMs: null,
  commentaryStyle: "FRIENDLY_MASTER",
  llmModel: "qwen2.5:7b-instruct",
};

function renderPanel(overrides: Partial<Parameters<typeof SettingsPanel>[0]> = {}) {
  const props = {
    settings,
    models: ["qwen2.5:7b-instruct", "gemma4:12b"],
    isLoadingModels: false,
    isStarting: false,
    hasActiveGame: false,
    onSettingsChange: vi.fn(),
    onStartGame: vi.fn(),
    ...overrides,
  };
  const result = render(<SettingsPanel {...props} />);
  return { ...result, props };
}

/** Управляемая панель — для проверки появления зависимых полей. */
function ControlledPanel() {
  const [value, setValue] = useState<StartGameRequest>(settings);
  return (
    <SettingsPanel
      settings={value}
      models={["qwen2.5:7b-instruct"]}
      isLoadingModels={false}
      isStarting={false}
      hasActiveGame={false}
      onSettingsChange={setValue}
      onStartGame={vi.fn()}
    />
  );
}

describe("SettingsPanel", () => {
  it("переключает цвет игрока", () => {
    const { props } = renderPanel();
    fireEvent.click(screen.getByRole("button", { name: /Чёрные/ }));
    expect(props.onSettingsChange).toHaveBeenCalledWith({
      ...settings,
      playerColor: "BLACK",
    });
  });

  it("помечает выбранные значения через aria-pressed", () => {
    renderPanel();
    expect(screen.getByRole("button", { name: /Белые/ })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByRole("button", { name: /Средне/ })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
  });

  it("показывает поля глубины и бюджета для своей сложности", () => {
    render(<ControlledPanel />);
    expect(screen.queryByLabelText("Глубина")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Свои" }));

    expect(screen.getByLabelText("Глубина")).toBeInTheDocument();
    expect(screen.getByLabelText("Бюджет, мс")).toBeInTheDocument();
  });

  it("выбирает режим CUSTOM с значениями по умолчанию", () => {
    const { props } = renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Свои" }));
    expect(props.onSettingsChange).toHaveBeenCalledWith({
      ...settings,
      difficulty: "CUSTOM",
      aiDepth: 6,
      aiTimeBudgetMs: 1500,
    });
  });

  it("меняет стиль комментария и модель", () => {
    const { props } = renderPanel();
    fireEvent.change(screen.getByLabelText("Стиль комментария"), {
      target: { value: "COMPETITIVE" },
    });
    expect(props.onSettingsChange).toHaveBeenCalledWith({
      ...settings,
      commentaryStyle: "COMPETITIVE",
    });

    fireEvent.change(screen.getByLabelText("Модель LLM"), {
      target: { value: "gemma4:12b" },
    });
    expect(props.onSettingsChange).toHaveBeenCalledWith({
      ...settings,
      llmModel: "gemma4:12b",
    });
  });

  it("запускает партию с текущими настройками", () => {
    const { props } = renderPanel();
    fireEvent.click(screen.getByRole("button", { name: /Начать партию/ }));
    expect(props.onStartGame).toHaveBeenCalledWith(settings);
  });

  it("блокирует кнопку, пока партия создаётся", () => {
    renderPanel({ isStarting: true });
    expect(
      screen.getByRole("button", { name: /Создаём партию/ }),
    ).toBeDisabled();
  });
});
