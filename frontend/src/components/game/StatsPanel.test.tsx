import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import StatsPanel from "./StatsPanel";

const stats = { humanWins: 3, aiWins: 2, draws: 1, total: 6 };

describe("StatsPanel", () => {
  it("показывает пустое состояние без завершённых партий", () => {
    render(
      <StatsPanel stats={null} isLoading={false} error={null} onRefresh={vi.fn()} />,
    );
    expect(
      screen.getByText(/Пока нет завершённых партий/),
    ).toBeInTheDocument();
  });

  it("показывает победы человека, ИИ и ничьи", () => {
    render(
      <StatsPanel stats={stats} isLoading={false} error={null} onRefresh={vi.fn()} />,
    );
    expect(screen.getByText("Вы")).toBeInTheDocument();
    expect(screen.getByText("ИИ")).toBeInTheDocument();
    expect(screen.getByText("Ничьи")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("Всего партий: 6")).toBeInTheDocument();
  });

  it("показывает ошибку загрузки статистики", () => {
    render(
      <StatsPanel
        stats={null}
        isLoading={false}
        error="Статистика недоступна: сеть"
        onRefresh={vi.fn()}
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(
      "Статистика недоступна",
    );
  });

  it("вызывает обновление по кнопке", () => {
    const onRefresh = vi.fn();
    render(
      <StatsPanel stats={stats} isLoading={false} error={null} onRefresh={onRefresh} />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Обновить" }));
    expect(onRefresh).toHaveBeenCalledTimes(1);
  });
});
