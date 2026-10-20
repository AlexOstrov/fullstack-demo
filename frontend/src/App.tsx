import { useCallback, useEffect, useRef, useState } from "react";
import "./App.css";

import {
  makeAiMove,
  makeMove,
  getLlmModels,
  startGame,
  type GameStateResponse,
  type StartGameRequest,
} from "./api/client";
import Board from "./components/game/Board";
import GameControls from "./components/game/GameControls";
import StatusBar from "./components/game/StatusBar";
import CommentaryBox from "./components/game/CommentaryBox";
import SettingsPanel from "./components/game/SettingsPanel";

/** Начальные настройки партии по умолчанию. */
const DEFAULT_PREFS: StartGameRequest = {
  playerColor: "WHITE",
  difficulty: "EASY",
  aiDepth: null,
  aiTimeBudgetMs: null,
  commentaryStyle: "FRIENDLY_MASTER",
  llmModel: "qwen2.5:7b-instruct",
};

/** Пустая доска 8x8. */
const emptyBoard: string[][] = Array.from({ length: 8 }, () =>
  Array.from({ length: 8 }, () => ""),
);

export default function App() {
  const [gameId, setGameId] = useState<string | null>(null);
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [settings, setSettings] = useState<StartGameRequest>(DEFAULT_PREFS);
  const [selectedCell, setSelectedCell] = useState<{
    row: number;
    col: number;
  } | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [models, setModels] = useState<string[]>([]);
  const [isLoadingModels, setIsLoadingModels] = useState(false);
  const aiTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  /** Загрузить список моделей LLM при старте. */
  useEffect(() => {
    setIsLoadingModels(true);
    getLlmModels()
      .then((resp) => {
        setModels(resp.models);
        if (
          !settings.llmModel ||
          !resp.models.includes(settings.llmModel)
        ) {
          setSettings((prev: StartGameRequest) => ({
            ...prev,
            llmModel: resp.defaultModel,
          }));
        }
      })
      .catch((err) => {
        setErrorMessage(
          err instanceof Error ? err.message : "Ошибка загрузки моделей LLM",
        );
      })
      .finally(() => setIsLoadingModels(false));
  }, []);

  /** Начать новую партию. */
  const startNewGame = useCallback(
    async (prefs: StartGameRequest) => {
      setErrorMessage(null);
      setStatusMessage(null);
      setSelectedCell(null);
      try {
        const state = await startGame(prefs);
        setGameId(state.gameId);
        setGameState(state);
        setSettings(prefs);
        if (state.currentPlayer === state.aiColor) {
          scheduleAiMove(state);
        }
      } catch (err) {
        setErrorMessage(
          err instanceof Error ? err.message : "Ошибка создания партии",
        );
      }
    },
    [],
  );

  /** Планировать ход ИИ. */
  const scheduleAiMove = useCallback(
    (state: GameStateResponse) => {
      if (aiTimerRef.current) {
        clearTimeout(aiTimerRef.current);
      }
      aiTimerRef.current = setTimeout(async () => {
        try {
          const newState = await makeAiMove(state.gameId);
          setGameState(newState);
        } catch (err) {
          setErrorMessage(
            err instanceof Error ? err.message : "Ошибка хода ИИ",
          );
        }
      }, 500);
    },
    [],
  );

  /** Обработка клика по клетке. */
  const handleCellClick = useCallback(
    (row: number, col: number) => {
      if (!gameState || !gameId) return;
      if (gameState.status !== "IN_PROGRESS") return;
      if (gameState.currentPlayer !== gameState.humanColor) return;

      const pieceToken = gameState.board[row][col];
      const isOwnPiece =
        pieceToken !== "" &&
        ((pieceToken.startsWith("W") &&
          gameState.humanColor === "WHITE") ||
          (pieceToken.startsWith("B") &&
            gameState.humanColor === "BLACK"));

      if (selectedCell) {
        // Есть выбранная шашка - пытаемся сделать ход
        const path: { row: number; col: number }[] = [
          { row: selectedCell.row, col: selectedCell.col },
          { row, col },
        ];
        setSelectedCell(null);
        makeMoveWithPath(gameId, path, gameState.moveNumber);
      } else if (isOwnPiece) {
        // Выбрать свою шашку
        setSelectedCell({ row, col });
      }
    },
    [gameState, gameId, selectedCell],
  );

  /** Выполнить ход игрока. */
  const makeMoveWithPath = useCallback(
    async (gid: string, path: { row: number; col: number }[], clientMoveNumber: number) => {
      setErrorMessage(null);
      try {
        const newState = await makeMove(gid, {
          path,
          clientMoveNumber,
        });
        setGameState(newState);
        setSelectedCell(null);
        if (newState.status === "IN_PROGRESS") {
          scheduleAiMove(newState);
        }
      } catch (err) {
        setErrorMessage(
          err instanceof Error ? err.message : "Ошибка хода",
        );
      }
    },
    [],
  );

  const humanColor = gameState?.humanColor ?? settings.playerColor;
  const currentPlayerColor = gameState?.currentPlayer ?? humanColor;
  const status = gameState?.status ?? "IN_PROGRESS";
  const endReason = gameState?.endReason ?? null;

  // Если игра начата и сейчас ход ИИ, планируем его
  useEffect(() => {
    if (!gameState || gameId === null) return;
    if (gameState.status !== "IN_PROGRESS") return;
    if (gameState.currentPlayer === gameState.aiColor) {
      scheduleAiMove(gameState);
    }
  }, [gameState, gameId, scheduleAiMove]);

  useEffect(() => {
    return () => {
      if (aiTimerRef.current) {
        clearTimeout(aiTimerRef.current);
      }
    };
  }, []);

  const isGameActive = gameId !== null && gameState !== null;

  const getStatusMessage = (state: GameStateResponse): string => {
    if (state.status === "IN_PROGRESS") {
      return state.currentPlayer === state.humanColor
        ? "Ваш ход"
        : "Ход ИИ...";
    }
    if (state.endReason === "RESIGN") {
      return state.currentPlayer === state.humanColor
        ? "Вы сдались — победа ИИ"
        : "ИИ сдался — ваша победа";
    }
    if (state.endReason === "DRAW_AGREEMENT") {
      return "Ничья по согласию";
    }
    if (state.endReason === "DRAW_REPETITION") {
      return "Ничья — троекратное повторение";
    }
    if (state.endReason === "DRAW_KINGS") {
      return "Ничья — только дамки";
    }
    return "Игра окончена";
  };

  return (
    <main className="game">
      <h1>Русские шашки</h1>

      <SettingsPanel
        settings={settings}
        models={models}
        isLoadingModels={isLoadingModels}
        onSettingsChange={setSettings}
        onStartGame={startNewGame}
        disabled={isGameActive}
      />

      <div className="game-board-wrapper">
        <StatusBar
          humanColor={humanColor}
          currentPlayerColor={currentPlayerColor}
          status={status}
          endReason={endReason}
          moveNumber={gameState?.moveNumber ?? 0}
          statusMessage={statusMessage}
          errorMessage={errorMessage}
        />

        <Board
          board={gameState?.board ?? emptyBoard}
          legalMoves={gameState?.legalMoves ?? []}
          currentPlayerColor={currentPlayerColor as "WHITE" | "BLACK"}
          humanColor={humanColor as "WHITE" | "BLACK"}
          selectedCell={selectedCell}
          onCellClick={handleCellClick}
        />

        <CommentaryBox
          ai={gameState?.ai ?? null}
          gameId={gameId ?? ""}
        />
      </div>

      <GameControls
        gameId={gameId ?? ""}
        status={status}
        endReason={endReason}
        currentPlayerColor={currentPlayerColor}
        humanColor={humanColor}
        preferences={settings}
        onGameOver={() => setStatusMessage(getStatusMessage(gameState!))}
        onRestart={startNewGame}
      />
    </main>
  );
}
