import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "./App";

describe("App (заглушка Sprint 0)", () => {
  it("показывает заголовок игры и статус партии", () => {
    render(<App />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Русские шашки" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Партия: не начата");
  });

  it("рендерит доску и подсказку о продолжении в Sprint 1", () => {
    render(<App />);
    expect(screen.getByRole("grid")).toBeInTheDocument();
    expect(
      screen.getByText(/API-интеграция.*в Sprint 1/),
    ).toBeInTheDocument();
  });

  it("не содержит демо-интерфейса items", () => {
    const { container } = render(<App />);
    expect(container.querySelector("table")).toBeNull();
    expect(screen.queryByPlaceholderText("Введите название")).toBeNull();
  });
});
