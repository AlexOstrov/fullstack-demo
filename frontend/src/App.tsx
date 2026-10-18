import Board from "./components/Board";
import "./App.css";

export default function App() {
  return (
    <main className="game">
      <h1>Русские шашки</h1>

      <div className="status-bar" role="status">
        <span>Партия: не начата</span>
        <span>Шашки: 12 : 12</span>
        <span>Ход: —</span>
      </div>

      <Board />

      <p className="game__hint">
        Заглушка доски (Sprint 0): API-интеграция, подсветка ходов и панель
        настроек — в Sprint 1.
      </p>
    </main>
  );
}
