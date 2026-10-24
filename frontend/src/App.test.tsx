import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "./App";

describe("App", () => {
  it("показывает заголовок игры", () => {
    render(<App />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Русские шашки" }),
    ).toBeInTheDocument();
  });

  it("рендерит панель настроек партии", () => {
    render(<App />);
    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Настройки партии",
      }),
    ).toBeInTheDocument();
  });

  it("рендерит доску 8×8 — ровно 64 клетки", () => {
    const { container } = render(<App />);
    expect(screen.getByRole("grid")).toBeInTheDocument();
    expect(container.querySelectorAll('[role="gridcell"]')).toHaveLength(64);
  });

  it("показывает начальную расстановку до старта партии", () => {
    const { container } = render(<App />);
    expect(container.querySelectorAll(".piece--white")).toHaveLength(12);
    expect(container.querySelectorAll(".piece--black")).toHaveLength(12);
  });

  it("имеет кнопку старта партии", () => {
    render(<App />);
    expect(
      screen.getByRole("button", { name: /Начать партию/ }),
    ).toBeInTheDocument();
  });

  it("показывает панели комментария и истории ходов", () => {
    render(<App />);
    expect(screen.getByText("Комментарий ИИ")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { level: 2, name: "История ходов" }),
    ).toBeInTheDocument();
  });

  it("показывает блок статистики побед", () => {
    render(<App />);
    expect(
      screen.getByRole("heading", { level: 2, name: "Счёт побед" }),
    ).toBeInTheDocument();
  });

  it("не содержит демо-интерфейса items", () => {
    const { container } = render(<App />);
    expect(container.querySelector("table")).toBeNull();
  });
});
