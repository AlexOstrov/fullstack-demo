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
        level: 3,
        name: "Настройки партии",
      }),
    ).toBeInTheDocument();
  });

  it("рендерит доску 8×8", () => {
    render(<App />);
    expect(screen.getByRole("grid")).toBeInTheDocument();
  });

  it("имеет кнопку старта партии", () => {
    render(<App />);
    expect(
      screen.getByRole("button", { name: /Начать партию/ }),
    ).toBeInTheDocument();
  });

  it("не содержит демо-интерфейса items", () => {
    const { container } = render(<App />);
    expect(container.querySelector("table")).toBeNull();
  });
});
