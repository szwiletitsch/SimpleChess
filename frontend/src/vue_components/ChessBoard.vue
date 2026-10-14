```vue
<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {createInitialGameState, getFen, loadFen} from '@/chess_logic/game'
import type {Square} from "@/chess_logic/types.ts";
import {analyze, getLegalMoves, makeMove} from "@/api/engineApi.ts";
import type {GameStatus} from "@/api/dto/engineDTO.ts";

// required for drawing chess field
const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
const ranks = [8, 7, 6, 5, 4, 3, 2, 1]
const pieceSymbols = {
  white: {
    king: '♔',
    queen: '♕',
    rook: '♖',
    bishop: '♗',
    knight: '♘',
    pawn: '♙',
  },
  black: {
    king: '♚',
    queen: '♛',
    rook: '♜',
    bishop: '♝',
    knight: '♞',
    pawn: '♟',
  },
}

// required to highlight fields accordingly
const game = reactive(createInitialGameState())
const selectedSquare = ref<Square | null>(null)
const legalMoves = ref<Square[]>([])
const gameStatus = ref<GameStatus>('IN_PROGRESS')

const positionHistory = ref<string[]>([
  getFen(game),
])

const currentPositionIndex = ref(0)

const isLatestPosition = computed(
    () => currentPositionIndex.value === positionHistory.value.length - 1,
)

const canGoBack = computed(
    () => currentPositionIndex.value > 0,
)

const canGoForward = computed(
    () => currentPositionIndex.value < positionHistory.value.length - 1,
)

async function selectSquare(rank: number, file: string) {
  if (!isLatestPosition.value) return
  if (gameStatus.value === 'CHECKMATE' ||
      gameStatus.value === 'STALEMATE') {
    return
  }

  const row = 8 - rank
  const col = files.indexOf(file)

  const square = {row, col}

  const piece = game.board[row]![col]
  if (piece && piece!.color === game.turn) {
    selectedSquare.value = square

    const fen = getFen(game)
    const legalSquares = (await getLegalMoves({
      fen,
      square: `${file}${rank}`,
    })).allowedSquares

    legalMoves.value = legalSquares.map(square => ({
      row: 8 - Number(square[1]),
      col: files.indexOf(square[0]!),
    }))


    return
  }

  if (!selectedSquare.value) return

  const isLegal = legalMoves.value.some(
      move => move.row === row && move.col === col,
  )

  if (isLegal) {
    const from =
        `${files[selectedSquare.value.col]}${8 - selectedSquare.value.row}`
    const to = `${file}${rank}`

    const moveResponse = await makeMove({
      fen: getFen(game),
      from,
      to,
    })

    // Apply White's move
    Object.assign(game, loadFen(moveResponse.fen))
    recordPosition(moveResponse.fen)

    gameStatus.value = moveResponse.status

    if (moveResponse.status === 'CHECKMATE' ||
        moveResponse.status === 'STALEMATE') {
      return
    }

    selectedSquare.value = null
    legalMoves.value = []

    // Let Black (the engine) respond
    const analysisResponse = await analyze({
      fen: moveResponse.fen,
      depth: 6,
    })

    // Apply Black's move
    Object.assign(game, loadFen(analysisResponse.fen))
    recordPosition(analysisResponse.fen)

    gameStatus.value = analysisResponse.status

    if (analysisResponse.status === 'CHECKMATE' ||
        analysisResponse.status === 'STALEMATE') {
      return
    }
  }
}

function handleRightClick(event: MouseEvent) {
  event.preventDefault()
  selectedSquare.value = null
}

function recordPosition(fen: string) {
  // Discard future positions if a new line starts
  positionHistory.value = positionHistory.value.slice(
      0,
      currentPositionIndex.value + 1,
  )

  positionHistory.value.push(fen)
  currentPositionIndex.value = positionHistory.value.length - 1
}

function goToPosition(index: number) {
  if (index < 0 || index >= positionHistory.value.length) {
    return
  }

  currentPositionIndex.value = index

  Object.assign(
      game,
      loadFen(positionHistory.value[index]!),
  )

  selectedSquare.value = null
  legalMoves.value = []
}

function goBack() {
  goToPosition(currentPositionIndex.value - 1)
}

function goForward() {
  goToPosition(currentPositionIndex.value + 1)
}

function undoLastTurn() {
  if (currentPositionIndex.value === 0) return

  const targetIndex = Math.max(0, currentPositionIndex.value - 2)
  goToPosition(targetIndex)

  // Remove the undone positions so they aren't part of the active game.
  positionHistory.value = positionHistory.value.slice(0, targetIndex + 1)
}

function getCapturedPieces(color: 'white' | 'black') {
  const startingCounts = {
    pawn: 8,
    knight: 2,
    bishop: 2,
    rook: 2,
    queen: 1,
    king: 1
  }

  const currentCounts = {
    pawn: 0,
    knight: 0,
    bishop: 0,
    rook: 0,
    queen: 0,
    king: 1
  }

  for (const piece of game.board.flat()) {
    if (piece?.color === color) {
      currentCounts[piece.type]++
    }
  }

  const symbols = pieceSymbols[color]
  const captured: string[] = []

  for (const type of Object.keys(startingCounts) as (keyof typeof startingCounts)[]) {
    const missing = Math.max(0, startingCounts[type] - currentCounts[type])

    for (let i = 0; i < missing; i++) {
      captured.push(symbols[type])
    }
  }

  return captured.join('')
}

// true = White's perspective
// false = Black's perspective
const whitesPerspective = true


const displayedFiles = computed(() =>
    whitesPerspective ? files : [...files].reverse()
)

const displayedRanks = computed(() =>
    whitesPerspective ? ranks : [...ranks].reverse()
)
</script>

<template>
  <div class="game-layout">
    <!-- Chessboard -->
    <div class="board-section">
      <div
          class="board-wrapper"
          @contextmenu="handleRightClick($event)"
      >
        <div class="rank-coordinates">
          <span
              v-for="rank in displayedRanks"
              :key="rank"
          >
            {{ rank }}
          </span>
        </div>

        <div class="board-container">
          <div class="board">
            <div
                v-for="(rank, rankIndex) in displayedRanks"
                :key="rank"
                class="rank"
            >
              <div
                  v-for="(file, fileIndex) in displayedFiles"
                  :key="`${file}${rank}`"
                  :class="{
                  selected: 8 - rank == selectedSquare?.row && files.indexOf(file) == selectedSquare?.col,
                    legal: legalMoves.some(move => move.row === 8 - rank && move.col === files.indexOf(file)),
                    light: (rank + files.indexOf(file)) % 2 === 0,
                    dark: (rank + files.indexOf(file)) % 2 === 1,
                  }"
                  class="square"
                  @click="selectSquare(rank, file)"
              >
                <span
                    v-if="game.board[8 - rank]![files.indexOf(file)]"
                    class="piece"
                >
                  {{
                    pieceSymbols[
                        game.board[8 - rank]![files.indexOf(file)]!.color
                        ][
                        game.board[8 - rank]![files.indexOf(file)]!.type
                        ]
                  }}
                </span>
              </div>
            </div>
          </div>

          <div class="file-coordinates">
            <span
                v-for="file in displayedFiles"
                :key="file"
            >
              {{ file }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Controls panel -->
    <aside class="controls-panel">
      <div class="panel-header">
        <div class="panel-icon">♟</div>
        <div>
          <h2>Game Controls</h2>
          <p>Chess match</p>
        </div>
      </div>

      <div class="panel-divider"></div>

      <!-- Game status -->
      <section class="control-section">
        <h3>GAME STATUS</h3>

        <div
            :class="{
            'status-check': gameStatus === 'CHECK',
            'status-finished':
              gameStatus === 'CHECKMATE' ||
              gameStatus === 'STALEMATE',
          }"
            class="status-card"
        >
          <span class="status-indicator"></span>

          <span v-if="gameStatus === 'IN_PROGRESS'">
            In progress
          </span>
          <span v-else-if="gameStatus === 'CHECK'">
            Check!
          </span>
          <span v-else-if="gameStatus === 'CHECKMATE'">
            Checkmate
          </span>
          <span v-else-if="gameStatus === 'STALEMATE'">
            Draw by stalemate
          </span>
          <span v-else>
            {{ gameStatus }}
          </span>
        </div>
      </section>

      <!-- Position navigation -->
      <section class="control-section">
        <h3>MOVE HISTORY</h3>

        <div class="position-card">
          <span class="position-label">Position</span>
          <div class="position-number">
            <strong>{{ currentPositionIndex + 1 }}</strong>
            <span>/ {{ positionHistory.length }}</span>
          </div>

          <div class="history-progress">
            <div
                :style="{
                width: `${
                  ((currentPositionIndex + 1) /
                    positionHistory.length) *
                  100
                }%`,
              }"
                class="history-progress-fill"
            ></div>
          </div>
        </div>

        <div class="navigation-buttons">
          <button
              :disabled="!canGoBack"
              class="control-button secondary"
              @click="goBack"
          >
            <span class="button-icon">←</span>
            Previous
          </button>

          <button
              :disabled="!canGoForward"
              class="control-button secondary"
              @click="goForward"
          >
            Next
            <span class="button-icon">→</span>
          </button>
        </div>

        <button
            :disabled="
            !isLatestPosition || currentPositionIndex === 0
          "
            class="control-button undo-button"
            @click="undoLastTurn"
        >
          <span class="button-icon">↶</span>
          Undo last turn
        </button>

        <p
            v-if="!isLatestPosition"
            class="history-hint"
        >
          Viewing a previous position. Return to the latest position
          to continue playing.
        </p>
      </section>

      <!-- Captured pieces -->
      <section class="control-section captured-section">
        <h3>CAPTURED PIECES</h3>

        <div class="captured-card">
          <div class="captured-heading">
            <span class="player-dot black-dot"></span>
            <span>Black pieces lost</span>
          </div>
          <div class="captured-list">
            <span v-if="getCapturedPieces('black')">
              {{ getCapturedPieces('black') }}
            </span>
            <span
                v-else
                class="empty-captured"
            >
              No captures yet
            </span>
          </div>
        </div>

        <div class="captured-card">
          <div class="captured-heading">
            <span class="player-dot white-dot"></span>
            <span>White pieces lost</span>
          </div>
          <div class="captured-list">
            <span v-if="getCapturedPieces('white')">
              {{ getCapturedPieces('white') }}
            </span>
            <span
                v-else
                class="empty-captured"
            >
              No captures yet
            </span>
          </div>
        </div>
      </section>
    </aside>
  </div>
</template>
```

```css
<style scoped>
.game-layout {
  display: flex;
  align-items: stretch; /* Both columns share the same height */
  justify-content: center;
  gap: 28px;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 5px;
  color: var(--chess-text);
}

.board-section {
  flex: 1 1 0;
  min-width: 0;
  max-width: 720px;
}


.board-wrapper {
  display: flex;
  width: 100%;
}

.rank-coordinates {
  flex: 0 0 24px;
  display: grid;
  grid-template-rows: repeat(8, 1fr);
  align-items: center;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--chess-board-coordinate);
}

.board-container {
  flex: 1;
  min-width: 0;
}

.board {
  display: grid;
  grid-template-rows: repeat(8, 1fr);
  aspect-ratio: 1;
  overflow: hidden;
  border: 4px solid var(--chess-board-frame);
  border-radius: 4px;
  box-shadow: var(--chess-board-shadow);
}

.rank {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  min-height: 0;
}

.square {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 0;
  cursor: pointer;
  transition: filter 120ms ease;
}

.square:hover {
  filter: brightness(1.08);
}

.light {
  background: var(--chess-board-light);
}

.dark {
  background: var(--chess-board-dark);
}

.selected {
  background: var(--chess-board-selected);
}

.legal::after {
  content: '';
  position: absolute;
  width: 28%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: var(--chess-board-legal);
  pointer-events: none;
}

.piece {
  position: relative;
  color: black;
  z-index: 1;
  font-size: clamp(1.2rem, 5.2vw, 4.5rem);
  line-height: 1;
  cursor: pointer;
  user-select: none;
}

.file-coordinates {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  height: 24px;
  align-items: center;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--chess-board-coordinate);
}

/* Controls panel */
.controls-panel {
  flex: 0 0 280px;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  padding: 22px;
  color: var(--chess-text);
  background: var(--chess-panel);
  border: 1px solid var(--chess-border);
  border-radius: 16px;
  box-shadow: var(--chess-shadow);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.panel-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  color: var(--chess-accent);
  background: var(--chess-accent-surface);
  font-size: 1.8rem;
}

.panel-header h2 {
  margin: 0;
  color: var(--chess-text);
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.panel-header p {
  margin: 5px 0 0;
  color: var(--chess-text-muted);
  font-size: 0.8rem;
}

.panel-divider {
  height: 1px;
  margin-bottom: 22px;
  background: var(--chess-divider);
}

.control-section {
  margin-bottom: 24px;
}

.control-section h3 {
  margin: 0 0 12px;
  color: var(--chess-text-muted);
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.12em;
}

/* Status */
.status-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  color: var(--chess-status-ok);
  background: var(--chess-status-ok-bg);
  border: 1px solid var(--chess-status-ok-border);
  border-radius: 10px;
  font-size: 0.88rem;
  font-weight: 600;
}

.status-indicator {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--chess-status-ok);
}

.status-check {
  color: var(--chess-status-warning);
  background: var(--chess-status-warning-bg);
  border-color: var(--chess-status-warning-border);
}

.status-check .status-indicator {
  background: var(--chess-status-warning);
}

.status-finished {
  color: var(--chess-status-danger);
  background: var(--chess-status-danger-bg);
  border-color: var(--chess-status-danger-border);
}

.status-finished .status-indicator {
  background: var(--chess-status-danger);
}

/* Position and progress */
.position-card {
  padding: 14px;
  margin-bottom: 12px;
  background: var(--chess-card);
  border: 1px solid var(--chess-border);
  border-radius: 10px;
}

.position-label {
  color: var(--chess-text-muted);
  font-size: 0.8rem;
}

.position-number {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 5px;
}

.position-number strong {
  color: var(--chess-text);
  font-size: 1.7rem;
  line-height: 1.3;
}

.position-number span {
  color: var(--chess-text-muted);
  font-size: 0.9rem;
}

.history-progress {
  height: 5px;
  margin-top: 12px;
  overflow: hidden;
  background: var(--chess-track);
  border-radius: 99px;
}

.history-progress-fill {
  height: 100%;
  background: var(--chess-accent);
  border-radius: inherit;
  transition: width 180ms ease;
}

/* Buttons */
.navigation-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 9px;
}

.control-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  font: inherit;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 150ms ease,
  border-color 150ms ease,
  transform 150ms ease,
  opacity 150ms ease;
}

.control-button:not(:disabled):active {
  transform: translateY(1px);
}

.control-button:focus-visible {
  outline: 2px solid var(--chess-focus);
  outline-offset: 3px;
}

.control-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.secondary {
  color: var(--chess-text);
  background: var(--chess-control);
  border-color: var(--chess-border-control);
}

.secondary:not(:disabled):hover {
  background: var(--chess-control-hover);
  border-color: var(--chess-border-hover);
}

.button-icon {
  font-size: 1.1rem;
  line-height: 1;
}

.undo-button {
  width: 100%;
  color: var(--chess-accent-text);
  background: var(--chess-accent-surface);
  border-color: var(--chess-accent-border);
}

.undo-button:not(:disabled):hover {
  background: var(--chess-accent-hover);
}

.history-hint {
  margin: 10px 2px 0;
  color: var(--chess-text-muted);
  font-size: 0.76rem;
  line-height: 1.5;
}

/* Captured pieces */
.captured-section {
  margin-bottom: 0;
}

.captured-card {
  padding: 12px;
  margin-bottom: 9px;
  background: var(--chess-card);
  border: 1px solid var(--chess-border);
  border-radius: 10px;
}

.captured-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--chess-text-secondary);
  font-size: 0.78rem;
  font-weight: 500;
}

.player-dot {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border-radius: 50%;
}

.black-dot {
  background: var(--chess-piece-black);
  border: 1px solid var(--chess-piece-outline);
}

.white-dot {
  background: var(--chess-piece-white);
  border: 1px solid var(--chess-piece-outline);
}

.captured-list {
  min-height: 30px;
  margin-top: 0;
  overflow-wrap: anywhere;
  font-family: 'Segoe UI Symbol', 'Noto Sans Symbols 2', sans-serif;
  font-size: 1.55rem;
  line-height: 1.4;
  letter-spacing: 1px;
}

.empty-captured {
  color: var(--chess-text-subtle);
  font-family: inherit;
  font-size: 0.76rem;
  font-style: italic;
  letter-spacing: 0;
}

/* Responsive layout */
@media (max-width: 1000px) {
  .game-layout {
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 16px;
  }

  .board-section {
    flex: none;
    width: 100%;
    max-width: 560px;
    min-width: 0;
  }

  .controls-panel {
    flex: none;
    align-self: center;
    width: 100%;
    max-width: 560px;
    box-sizing: border-box;
  }
}
</style>
```