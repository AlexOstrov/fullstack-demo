import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import "./App.css";

import {
  getLlmModels,
  getMoves,
  getTournamentStats,
  makeAiMove,
  makeMove,
  startGame,
} from "./api/client";
import type {
  Difficulty,
  GameStateResponse,
  MoveRecordDto,
  PlayerColor,
  StartGameRequest,
  TournamentStatsResponse,
} from "./api/types";
import Board from "./components/game/Board";
import CommentaryBox from "./components/game/CommentaryBox";
import CrownIcon from "./components/game/CrownIcon";
import GameControls from "./components/game/GameControls";
import MoveHistory from "./components/game/MoveHistory";
import PlayerCard from "./components/game/PlayerCard";
import SettingsPanel from "./components/game/SettingsPanel";
import StatsPanel from "./components/game/StatsPanel";
import StatusBar from "./components/game/StatusBar";
import { isOwnPieceToken } from "./components/game/tokens";

/** Начальные настройки партии по умолчанию. */
const DEFAULT_PREFS: StartGameRequest = {
  playerColor: "WHITE",
  difficulty: "EASY",
  aiDepth: null,
  aiTimeBudgetMs: null,
  commentaryStyle: "FRIENDLY_MASTER",
  llmModel: "qwen2.5:7b-instruct",
};

const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  EASY: "Легко",
  MEDIUM: "Средне",
  HARD: "Сложно",
  CUSTOM: "Свои настройки",
};

/** Начальная расстановка — превью до старта партии. */
function createPreviewBoard(): string[][] {
  const board = Array.from({ length: 8 }, () =>
    Array.from({ length: 8 }, () => ""),
  );
  for (let row = 0; row < 3; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) board[row][col] = "b";
    }
  }
  for (let row = 5; row < 8; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      if ((row + col) % 2 === 1) board[row][col] = "w";
    }
  }
  return board;
}

const PREVIEW_BOARD = createPreviewBoard();

function colorOfToken(token: string): PlayerColor {
  return token === "w" || token === "W" ? "WHITE" : "BLACK";
}

function isKingToken(token: string): boolean {
  return token === "W" || token === "B";
}

/** Вычислить анимацию хода между двумя досками. */
function computeMoveAnimation(
  oldBoard: string[][],
  newBoard: string[][],
): {
  fromRow: number;
  fromCol: number;
  toRow: number;
  toCol: number;
  type: "move" | "capture" | "promote";
} | null {
  const changedFrom: { row: number; col: number; piece: string }[] = [];
  const changedTo: { row: number; col: number; piece: string }[] = [];

  for (let row = 0; row < 8; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      const oldPiece = oldBoard[row][col];
      const newPiece = newBoard[row][col];
      if (oldPiece === newPiece) continue;
      if (oldPiece !== "") changedFrom.push({ row, col, piece: oldPiece });
      if (newPiece !== "") changedTo.push({ row, col, piece: newPiece });
    }
  }

  if (changedFrom.length === 0 || changedTo.length === 0) return null;

  // Перемещаемая шашка: единственная, чей цвет совпадает с цветом
  // появившейся шашки (остальные изменения — снятые при взятии шашки).
  let fromCell: { row: number; col: number; piece: string } | null = null;
  let toCell: { row: number; col: number; piece: string } | null = null;

  for (const from of changedFrom) {
    const match = changedTo.find(
      (to) => colorOfToken(to.piece) === colorOfToken(from.piece),
    );
    if (match) {
      fromCell = from;
      toCell = match;
      break;
    }
  }

  if (!fromCell || !toCell) return null;

  const movedFar =
    Math.abs(toCell.row - fromCell.row) > 1 ||
    Math.abs(toCell.col - fromCell.col) > 1;

  const type =
    isKingToken(toCell.piece) && !isKingToken(fromCell.piece)
      ? "promote"
      : movedFar || changedFrom.length > 1
        ? "capture"
        : "move";

  return {
    fromRow: fromCell.row,
    fromCol: fromCell.col,
    toRow: toCell.row,
    toCol: toCell.col,
    type,
  };
}

export default function App() {
  const [gameId, setGameId] = useState<string | null>(null);
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [settings, setSettings] = useState<StartGameRequest>(DEFAULT_PREFS);
  const [gamePrefs, setGamePrefs] = useState<StartGameRequest>(DEFAULT_PREFS);
  const [selectedCell, setSelectedCell] = useState<{
    row: number;
    col: number;
  } | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [models, setModels] = useState<string[]>([]);
  const [isLoadingModels, setIsLoadingModels] = useState(false);
  const [isStarting, setIsStarting] = useState(false);
  const [history, setHistory] = useState<MoveRecordDto[]>([]);
  const [stats, setStats] = useState<TournamentStatsResponse | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);

  const aiTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const animationRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const activeGameIdRef = useRef<string | null>(null);

  // Состояние для анимации хода
  const [prevBoard, setPrevBoard] = useState<string[][]>(PREVIEW_BOARD);
  const [displayBoard, setDisplayBoard] = useState<string[][]>(PREVIEW_BOARD);
  const [animation, setAnimation] = useState<{
    fromRow: number;
    fromCol: number;
    toRow: number;
    toCol: number;
    type: "move" | "capture" | "promote";
  } | null>(null);

  /** Загрузить список моделей LLM при старте. */
  useEffect(() => {
    setIsLoadingModels(true);
    getLlmModels()
      .then((response) => {
        setModels(response.models);
        setSettings((prev) => ({
          ...prev,
          llmModel:
            response.models.length === 0 ||
            response.models.includes(prev.llmModel)
              ? prev.llmModel
              : response.defaultModel,
        }));
      })
      .catch((err) => {
        setErrorMessage(
          err instanceof Error
            ? `Не удалось получить список моделей LLM (${err.message}). Будет использована модель по умолчанию.`
            : "Не удалось получить список моделей LLM. Будет использована модель по умолчанию.",
        );
      })
      .finally(() => setIsLoadingModels(false));
  }, []);

  /** Загрузить статистику завершённых партий (B1). */
  const refreshStats = useCallback(async () => {
    setIsLoadingStats(true);
    try {
      const response = await getTournamentStats();
      setStats(response);
      setStatsError(null);
    } catch (err) {
      setStatsError(
        err instanceof Error
          ? `Статистика недоступна: ${err.message}`
          : "Статистика недоступна",
      );
    } finally {
      setIsLoadingStats(false);
    }
  }, []);

  useEffect(() => {
    void refreshStats();
  }, [refreshStats]);

  /** Начать новую партию с выбранными настройками. */
  const startNewGame = useCallback(async (prefs: StartGameRequest) => {
    setErrorMessage(null);
    setSelectedCell(null);
    setAnimation(null);
    setHistory([]);
    setGameState(null);
    setGameId(null);
    activeGameIdRef.current = null;
    setDisplayBoard(PREVIEW_BOARD);
    setPrevBoard(PREVIEW_BOARD);
    if (aiTimerRef.current) {
      clearTimeout(aiTimerRef.current);
      aiTimerRef.current = null;
    }
    if (animationRef.current) {
      clearTimeout(animationRef.current);
      animationRef.current = null;
    }

    setIsStarting(true);
    try {
      const state = await startGame(prefs);
      activeGameIdRef.current = state.gameId;
      setGameId(state.gameId);
      setGameState(state);
      setSettings(prefs);
      setGamePrefs(prefs);
      setPrevBoard(state.board);
      setDisplayBoard(state.board);
    } catch (err) {
      setErrorMessage(
        err instanceof Error
          ? `Не удалось создать партию: ${err.message}`
          : "Не удалось создать партию",
      );
    } finally {
      setIsStarting(false);
    }
  }, []);

  /** Выполнить ход игрока по полному пути (включая серийные взятия). */
  const makeMoveWithPath = useCallback(
    async (
      gid: string,
      path: { row: number; col: number }[],
      clientMoveNumber: number,
    ) => {
      setErrorMessage(null);
      try {
        const next = await makeMove(gid, { path, clientMoveNumber });
        if (activeGameIdRef.current !== gid) return;
        setGameState(next);
        setSelectedCell(null);
      } catch (err) {
        if (activeGameIdRef.current !== gid) return;
        setErrorMessage(err instanceof Error ? err.message : "Ошибка хода");
      }
    },
    [],
  );

  /** Обработка клика по клетке. */
  const handleCellClick = useCallback(
    (row: number, col: number) => {
      if (!gameState || !gameId) return;
      if (gameState.status !== "IN_PROGRESS") return;
      if (gameState.currentPlayer !== gameState.humanColor) return;

      const token = gameState.board[row]?.[col] ?? "";
      const isOwnPiece = isOwnPieceToken(token, gameState.humanColor);
      const isSameCell =
        selectedCell?.row === row && selectedCell?.col === col;

      if (selectedCell) {
        const move = gameState.legalMoves.find((candidate) => {
          if (candidate.path.length < 2) return false;
          const start = candidate.path[0];
          const end = candidate.path[candidate.path.length - 1];
          return (
            start.row === selectedCell.row &&
            start.col === selectedCell.col &&
            end.row === row &&
            end.col === col
          );
        });

        if (move) {
          setSelectedCell(null);
          void makeMoveWithPath(gameId, move.path, gameState.moveNumber);
          return;
        }

        if (isOwnPiece) {
          setSelectedCell(isSameCell ? null : { row, col });
          return;
        }

        setSelectedCell(null);
        return;
      }

      if (isOwnPiece) {
        setSelectedCell({ row, col });
      }
    },
    [gameState, gameId, selectedCell, makeMoveWithPath],
  );

  /** Обновить состояние после сдачи/ничьей. */
  const handleStateUpdate = useCallback((state: GameStateResponse) => {
    setErrorMessage(null);
    setGameState(state);
    setSelectedCell(null);
  }, []);

  const handleError = useCallback((message: string) => {
    setErrorMessage(message);
  }, []);

  // Анимация перехода между двумя состояниями доски
  useEffect(() => {
    if (!gameState) return;

    const anim = computeMoveAnimation(prevBoard, gameState.board);
    if (animationRef.current) {
      clearTimeout(animationRef.current);
      animationRef.current = null;
    }

    if (anim) {
      setAnimation(anim);
      animationRef.current = setTimeout(
        () => {
          setDisplayBoard(gameState.board);
          setPrevBoard(gameState.board);
          setAnimation(null);
          animationRef.current = null;
        },
        anim.type === "capture" ? 430 : 300,
      );
    } else {
      setAnimation(null);
      setDisplayBoard(gameState.board);
      setPrevBoard(gameState.board);
    }
  }, [gameState, prevBoard]);

  // Ход ИИ, когда очередь за соперником
  useEffect(() => {
    if (!gameState || gameId === null) return;
    if (gameState.status !== "IN_PROGRESS") return;
    if (gameState.currentPlayer !== gameState.aiColor) return;

    const gid = gameState.gameId;
    aiTimerRef.current = setTimeout(async () => {
      try {
        const next = await makeAiMove(gid);
        if (activeGameIdRef.current !== gid) return;
        setGameState(next);
      } catch (err) {
        if (activeGameIdRef.current !== gid) return;
        setErrorMessage(
          err instanceof Error ? err.message : "Ошибка хода ИИ",
        );
      }
    }, 350);

    return () => {
      if (aiTimerRef.current) {
        clearTimeout(aiTimerRef.current);
        aiTimerRef.current = null;
      }
    };
  }, [gameState, gameId]);

  // История ходов: обновляется после каждого изменения партии
  useEffect(() => {
    if (!gameId) {
      setHistory([]);
      return;
    }
    let cancelled = false;
    getMoves(gameId)
      .then((response) => {
        if (!cancelled) setHistory(response.moves);
      })
      .catch(() => {
        // История не критична — не мешаем игре.
      });
    return () => {
      cancelled = true;
    };
  }, [gameId, gameState]);

  useEffect(() => {
    return () => {
      if (aiTimerRef.current) clearTimeout(aiTimerRef.current);
      if (animationRef.current) clearTimeout(animationRef.current);
    };
  }, []);

  const humanColor: PlayerColor =
    gameState?.humanColor ?? settings.playerColor;
  const aiColor: PlayerColor =
    gameState?.aiColor ?? (humanColor === "WHITE" ? "BLACK" : "WHITE");
  const currentPlayerColor = gameState?.currentPlayer ?? humanColor;
  const status = gameState?.status ?? "IN_PROGRESS";
  const inProgress = status === "IN_PROGRESS" && gameState !== null;
  const isHumanTurn = currentPlayerColor === humanColor;
  const isAiThinking = inProgress && !isHumanTurn;
  const canInteract = inProgress && isHumanTurn;
  const isFinished = gameState !== null && status !== "IN_PROGRESS";

  // Статистика меняется только по завершении партии — обновляем после результата.
  useEffect(() => {
    if (isFinished) {
      void refreshStats();
    }
  }, [isFinished, refreshStats]);

  const activePrefs = gameState ? gamePrefs : settings;
  const colorLabel = (color: PlayerColor) =>
    color === "WHITE" ? "Белые" : "Чёрные";

  const counts = useMemo(() => {
    const source = gameState?.board ?? displayBoard;
    let white = 0;
    let black = 0;
    for (const row of source) {
      for (const token of row) {
        if (token === "w" || token === "W") white += 1;
        else if (token === "b" || token === "B") black += 1;
      }
    }
    return { WHITE: white, BLACK: black } as Record<PlayerColor, number>;
  }, [gameState, displayBoard]);

  let headerLabel = "Партия не начата";
  let headerTone = "";
  if (gameState) {
    const whiteWon = status === "WHITE_WON";
    const humanWon =
      (whiteWon && humanColor === "WHITE") ||
      (status === "BLACK_WON" && humanColor === "BLACK");
    if (status === "DRAW") {
      headerLabel = "Ничья";
    } else if (!inProgress) {
      headerLabel = humanWon ? "Победа" : "Поражение";
      headerTone = humanWon ? " chip--accent" : " chip--danger";
    } else if (isHumanTurn) {
      headerLabel = "Ваш ход";
      headerTone = " chip--accent";
    } else {
      headerLabel = "ИИ думает";
      headerTone = " chip--accent";
    }
  }

  return (
    <main className="app">
      <header className="app-header">
        <div className="app-header__inner">
          <div className="brand">
            <span className="brand__mark" aria-hidden="true">
              <CrownIcon />
            </span>
            <div className="brand__text">
              <h1 className="brand__title">Русские шашки</h1>
              <span className="brand__subtitle">
                Классическая игра против ИИ
              </span>
            </div>
          </div>

          <div className="app-header__status">
            <span className={`chip${headerTone}`}>
              <span className="chip__dot" aria-hidden="true" />
              {headerLabel}
            </span>
            {gameState && (
              <span className="chip chip--muted">
                Ход №{gameState.moveNumber}
              </span>
            )}
          </div>
        </div>
      </header>

      <div className="layout">
        <aside className="layout__settings">
          <SettingsPanel
            settings={settings}
            models={models}
            isLoadingModels={isLoadingModels}
            isStarting={isStarting}
            hasActiveGame={gameState !== null}
            onSettingsChange={setSettings}
            onStartGame={startNewGame}
          />
          <StatsPanel
            stats={stats}
            isLoading={isLoadingStats}
            error={statsError}
            onRefresh={refreshStats}
          />
        </aside>

        <section className="layout__board" aria-label="Игровая зона">
          <div className="board-stage">
            <StatusBar
              hasGame={gameState !== null}
              humanColor={humanColor}
              currentPlayerColor={currentPlayerColor}
              status={status}
              endReason={gameState?.endReason ?? null}
              moveNumber={gameState?.moveNumber ?? 0}
              forcedCapture={gameState?.forcedCapture ?? false}
              errorMessage={errorMessage}
            />

            <PlayerCard
              label="ИИ-соперник"
              meta={`${colorLabel(aiColor)} · ${activePrefs.llmModel}`}
              color={aiColor}
              pieceCount={counts[aiColor]}
              isTurn={isAiThinking}
              isThinking
            />

            <Board
              board={displayBoard}
              legalMoves={gameState?.legalMoves ?? []}
              currentPlayerColor={currentPlayerColor}
              humanColor={humanColor}
              selectedCell={selectedCell}
              lastMove={gameState?.lastMove ?? null}
              onCellClick={handleCellClick}
              animatingFrom={
                animation
                  ? { row: animation.fromRow, col: animation.fromCol }
                  : null
              }
              animatingTo={
                animation
                  ? { row: animation.toRow, col: animation.toCol }
                  : null
              }
              animationType={animation?.type ?? null}
              interactive={canInteract}
            />

            <PlayerCard
              label="Вы"
              meta={`${colorLabel(humanColor)} · ${DIFFICULTY_LABEL[activePrefs.difficulty]}`}
              color={humanColor}
              pieceCount={counts[humanColor]}
              isTurn={inProgress && isHumanTurn}
            />

            <GameControls
              gameId={gameId ?? ""}
              status={status}
              canAct={canInteract}
              onStateUpdate={handleStateUpdate}
              onError={handleError}
            />
          </div>
        </section>

        <aside className="layout__info">
          <CommentaryBox
            ai={gameState?.ai ?? null}
            gameId={gameId ?? ""}
            isThinking={isAiThinking}
          />
          <MoveHistory moves={history} />
        </aside>
      </div>
    </main>
  );
}
